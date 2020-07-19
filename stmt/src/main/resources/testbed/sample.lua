local Util = require("testbed.util")
local Exec = require("testbed.exec")
local Inspect = require("inspect")

local POINTS = { 57, 783, 3722, 6951, 8704 }

local function flushOne(stmt)
    if stmt.samples then
        for point, sample in pairs(stmt.samples) do
            io.write(('>%d;%d\n'):format(point, stmt.stmtId, point))
            io.write(sample)
            io.write('\n')
        end
    end
end

local function flush(stmts, start, stop)
    start = start or 1
    stop = stop or #stmts
    for i = start, stop do
        flushOne(stmts[i])
    end
end

local function sampleStmtAtPoint(stmt, point, wtune)
    local status, _, res = Exec(stmt, point, wtune)

    if not status then
        Util.log(('[Sample] error in %s-%s (point = %s)\n'):format(wtune.app, stmt.stmtId, point), 0)
        Util.log(Inspect(res) .. '\n', 0)
        error(res)
    end

    local numRows
    numRows, stmt.samples[point] = Util.processResultSet(res)
    return numRows
end

local function sampleStmt(stmt, wtune)
    stmt.samples = {}
    local numRows = 0
    for _, point in ipairs(POINTS) do
        numRows = numRows + sampleStmtAtPoint(stmt, point, wtune)
    end
    return numRows
end

local function sampleStmts(stmts, wtune)
    local filter = wtune.stmtFilter

    if wtune.dump then
        Util.log('[Sample] writing to file\n', 5)
        io.output(wtune:appFile("sample", "w"))
    end

    local totalStmts = #stmts
    Util.log(('[Sample] %d statements to sample\n'):format(totalStmts), 1)
    Util.log('[Sample] ', 1)

    local numRows = 0
    local next = 1
    local waterMarker = 0

    for i, stmt in ipairs(stmts) do
        local curMarker = math.floor((i / totalStmts) * 10)
        if curMarker ~= waterMarker then
            Util.log('.', 1)
            waterMarker = curMarker
        end

        if not filter or filter(stmt) then
            numRows = numRows + sampleStmt(stmt, wtune)
            if numRows >= 1000000 then
                flush(stmts, next, i)
                numRows = 0
                next = i + 1
            end
        end
    end

    Util.log('\n', 1)

    flush(stmts, next, #stmts)
    io.output():flush()
    io.output():close()
    io.output(io.stdout)
end

return sampleStmts