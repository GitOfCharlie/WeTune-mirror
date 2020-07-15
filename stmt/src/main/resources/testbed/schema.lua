local Util = require("testbed.util")

local Schema = {}

local Constraint = {}

function Constraint:makeBase(type)
    local constraint = { type = type }
    setmetatable(constraint, self)
    self.__index = self
    return constraint
end

local PrimaryKey = Constraint:makeBase("primary")
local UniqueKey = Constraint:makeBase("unique")
local ForeignKey = Constraint:makeBase("foreign")

Constraint.Types = {
    primary = PrimaryKey,
    unique = UniqueKey,
    foreign = ForeignKey
}

function Constraint:isPrimary()
    return self.type == "primary"
end

function Constraint:isUnique()
    return self.type == "unique"
end

function Constraint:isForeign()
    return self.type == "foreign"
end

function Constraint:make(type, index, total)
    local constraintType = Constraint.Types[type:lower()]
    if not constraintType then
        return nil
    end
    local constraint = { index = index, total = total }
    setmetatable(constraint, constraintType)
    constraintType.__index = constraintType
    return constraint
end

function Constraint:priority()
    if self.type == "primary" then
        return 1
    elseif self.type == "unique" then
        return 2
    elseif self.type == "foreign" then
        return 3
    end
end

function Constraint.compare(x, y)
    local uniqueX = x:isPrimary() or x:isUnique()
    local uniqueY = y:isPrimary() or y:isUnique()

    if uniqueX and not uniqueY then
        return true
    elseif not uniqueX and uniqueY then
        return false
    else
        return x.index < y.index
    end
end

--function Constraint:__lt(x, y)
--    return x:priority() < y:priority()
--end

function PrimaryKey:modifyValue(lineNumber, origValue, maxLine)
    if self.total == 1 then
        return lineNumber
    end

    local base
    if maxLine <= 10000 then
        base = 100
    elseif maxLine <= 1000000 then
        base = 10000
    end

    if self.index == 1 then
        return math.ceil(lineNumber / base)
    else
        return math.fmod(lineNumber, base) + 1
    end
end

function UniqueKey:modifyValue(lineNumber, origValue, maxLine)
    if self.total == 1 then
        return lineNumber
    end

    local base
    if maxLine <= 10000 then
        base = 100
    elseif maxLine <= 1000000 then
        base = 10000
    end

    if self.index == 1 then
        return math.ceil(lineNumber / base)
    else
        return math.fmod(lineNumber, base) + 1
    end
end

function ForeignKey:modifyValue(lineNumber, origValue, maxLine)
    math.randomseed(lineNumber)
    math.random()
    return math.fmod(origValue + math.random(1, maxLine), maxLine) + 1
end

local DataType = {}

function DataType:make(desc)
    local dataType = desc
    setmetatable(dataType, self)
    self.__index = self
    return dataType
end

function DataType:numUniqueValues(maxLine)
    local category = self.category
    local ret

    if category == "integral" then
        ret = maxLine / 100

    elseif category == "fraction" then
        ret = maxLine / 80

    elseif category == "string" or category == "time" then
        ret = maxLine / 50

    end

    return math.max(3, math.ceil(ret))
end

function DataType:modifyValue(lineNumber, origValue, maxLine)
    local category = self.category
    if category == "integral" then
        return math.fmod(origValue, self:numUniqueValues(maxLine))

    elseif category == "fraction" then
        return math.fmod(origValue, self:numUniqueValues(maxLine))

    elseif category == "bit_string" then
        if self.width == 1 then
            return math.fmod(origValue, 2)
        else
            return origValue
        end

    elseif category == "boolean" then
        return math.fmod(origValue, 2)

    elseif category == "enum" then
        return math.fmod(origValue, #self.values)

    elseif category == "string" or category == "time" then
        return math.fmod(origValue, self:numUniqueValues(maxLine))

    else
        return origValue
    end
end

function DataType:convertType(value, dbType)
    if type(value) ~= 'number' then
        return value
    end

    local category = self.category
    local name = self.name
    local width = self.width
    local precision = self.precision

    local ret
    if category == "integral" then
        if name == "tinyint" then
            ret = math.fmod(value, 128)
        else
            ret = value
        end

    elseif category == "fraction" then
        ret = value
        if width > 0 and precision > 0 then
            width = width - precision
            if width <= 6 then
                ret = math.fmod(ret, math.pow(10, width))
            end
        end

    elseif category == "bit_string" then
        ret = value

    elseif category == "boolean" then
        if value == 0 then
            ret = "FALSE"
        else
            ret = "TRUE"
        end

    elseif category == "enum" then
        ret = "'" .. self.values[value] .. "'"

    elseif category == "string" then
        if width == -1 or width > 5 then
            width = 5
        end

        ret = string.format("'%0" .. width .. "d'", value)

    elseif category == "time" then
        ret = Util.ts2Str(Util.int2Ts(Util.baseTs() + 43200 * (value - 100)))
        if dbType == "pgsql" then
            ret = ret .. "::timestamp"
        end

    elseif category == "blob" then
        ret = string.format("0x%x", value)

    elseif category == "json" then
        ret = string.format("'{id = %d}'::json", value)

    elseif category == "inet" then
        local _0 = math.fmod(value, 256)
        local _1 = math.fmod(math.ceil(value / 256), 256)
        local _2 = math.fmod(value / 65536)
        ret = string.format("'127.%d.%d.%d'::inet", _2, _1, _0)

    elseif category == "uuid" then
        -- https://stackoverflow.com/questions/12505158/generating-a-uuid-in-postgres-for-insert-statement
        ret = "uuid_in(md5(random()::text || clock_timestamp()::text)::cstring)"

    elseif name == "tsvector" then
        ret = string.format("'%d'::tsvector", value)

    elseif name == "bytea" then
        ret = string.format("'%d'::bytea", value)

    end

    return ret
end

local Table = {}

function Table:make(tableName)
    local table = { tableName = tableName, columns = {} }
    setmetatable(table, self)
    self.__index = self
    return table
end

local Column = {}

function Column:make(columnName, dataTypeDesc)
    local column = { columnName = columnName,
                     dataType = DataType:make(dataTypeDesc),
                     constraints = {} }
    setmetatable(column, self)
    self.__index = self
    return column
end

function Column:uniqueIndex()
    for _, constraint in ipairs(self.constraints) do
        if constraint:isUnique() then
            return constraint.index
        end
    end
    return nil
end

function Column:primaryIndex()
    for _, constraint in ipairs(self.constraints) do
        if constraint:isPrimary() then
            return constraint.index
        end
    end
    return nil
end

function Column:foreignIndex()
    for _, constraint in ipairs(self.constraints) do
        if constraint:isPrimary() then
            return constraint.index
        end
    end
    return nil
end

function Column:guessBoolean()
    local ret = self.isBoolean or self.columnName:match("ed$") or self.columnName:match("able$")
            or self.dataType.width == 1
    self.isBoolean = ret
    return ret
end

function Column:guessEnum()
    local ret = self.isEnum or self.columnName:match("type$")
    self.isEnum = ret
    return ret
end

function Column:modifyValue(lineNumber, origValue, maxLine)
    if self:guessBoolean() then
        return origValue % 2
    elseif self:guessEnum() then
        return origValue % 8
    else
        return origValue
    end
end

function Column:valueAt(lineNumber, randSeq, maxLine, dbType)
    local value = randSeq:gen(self, lineNumber)
    --print("1. ", value)
    if #self.constraints == 0 then
        value = self.dataType:modifyValue(lineNumber, value, maxLine)
        value = self:modifyValue(lineNumber, value, maxLine)
    else
        value = self.constraints[1]:modifyValue(lineNumber, value, maxLine)
    end
    --print("2. ", value)
    --print("3. ", value)
    value = self.dataType:convertType(value, dbType)
    --print("4. ", value)
    return value
end

function Schema:addTable(tableDesc)
    local newTable = Table:make(tableDesc.tableName:lower())
    self.tables[newTable.tableName] = newTable

    for _, columnDesc in ipairs(tableDesc.columns) do
        local column = Column:make(columnDesc.columnName:lower(), columnDesc.dataType)
        newTable.columns[column.columnName] = column
    end

    for _, constrDesc in ipairs(tableDesc.constraints) do
        local parts = constrDesc.columns
        for i, part in ipairs(parts) do
            local column = newTable.columns[part]
            local constraint = Constraint:make(constrDesc.type, i, #parts)
            if constraint then
                table.insert(column.constraints, constraint)
            end
        end
    end

    for _, column in pairs(newTable.columns) do
        table.sort(column.constraints, Constraint.compare)
    end

    return newTable
end

function Schema:buildFrom(schemaDesc)
    for _, t in ipairs(schemaDesc) do
        self:addTable(t)
    end
    return self
end

function Schema:make(appName)
    local schema = { appName = appName, tables = {} }
    setmetatable(schema, self)
    self.__index = self
    return schema
end

return Schema
