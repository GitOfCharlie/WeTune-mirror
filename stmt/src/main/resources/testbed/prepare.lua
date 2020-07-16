local Util = require("testbed.util")
local TableGen = require("testbed.tablegen")

local function doInsert(con, numLines)
    return function(insertSql, tableName, ordinal, rowGen)
        Util.log(string.format("[Prepare] %03d. %s", ordinal, tableName), 1)

        con:bulk_insert_init(insertSql)

        for i = 1, numLines do
            if i % 2000 == 0 and i % 10000 ~= 0 then
                Util.log(".", 1)
            end

            if i % 2000 == 0 then
                con:bulk_insert_done()
                con:bulk_insert_init(insertSql)
            end

            if i % 10000 == 0 then
                Util.log(i)
            end

            con:bulk_insert_next(string.format('(%s)', table.concat(rowGen(i), ', ')))
        end

        con:bulk_insert_done()
        Util.log(" done\n", 1)
    end
end

local function doDump(con, numLines)
    return function(insertSql, tableName, ordinal, rowGen)
        for i = 1, numLines do
            Util.log(string.format('%s (%s)\n', insertSql, table.concat(rowGen(i), ', ')), 1)
        end
    end
end

local function populateTable(t, ordinal, wtune)
    local con = wtune.con
    local numLines = wtune.rows
    local randSeq = wtune.randSeq
    local dbType = wtune. dbType

    local func = con and doInsert or doDump
    TableGen:make(numLines, randSeq, dbType):genTable(t, ordinal, func(con, numLines))
end

--local function populateDb(con, tables, numLines, randSeq, dbType, filter)
local function populateDb(tables, wtune)
    local ordinal = 0
    local filter = wtune.tableFilter

    for _, t in pairs(tables) do
        ordinal = ordinal + 1
        if not filter or filter(ordinal, t) then
            populateTable(t, ordinal, wtune)
        end
    end
end

return populateDb
