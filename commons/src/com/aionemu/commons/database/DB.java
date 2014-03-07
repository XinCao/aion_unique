/*
 * This file is part of aion-emu <aion-emu.com>.
 *
 *  aion-emu is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-emu is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.commons.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * JDBC 的基本封装（连接池，采用common.dbcp）
 * 
 * @author caoxin
 */
public final class DB {

    protected static final Logger log = Logger.getLogger(DB.class);

    private DB() {
    }

    /**
     * Executes Select Query. Uses ReadSth to utilize params and return data.
     * Recycles connection after competion.
     *
     * @param query
     * @param reader
     * @return boolean Success
     */
    public static boolean select(String query, ReadStH reader) {
        return select(query, reader, null);
    }

    /**
     * Executes Select Query. Uses ReadSth to utilize params and return data.
     * Recycles connection after completion.
     *
     * @param query
     * @param reader
     * @param errMsg
     * @return boolean Success
     */
    public static boolean select(String query, ReadStH reader, String errMsg) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rset;
        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareStatement(query);
            if (reader instanceof ParamReadStH) {
                ((ParamReadStH) reader).setParams(stmt);
            }
            rset = stmt.executeQuery();
            reader.handleRead(rset);
        } catch (Exception e) {
            if (errMsg == null) {
                log.warn("Error executing select query " + e, e);
            } else {
                log.warn(errMsg + " " + e, e);
            }
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                log.warn("Failed to close DB connection " + e, e);
            }
        }
        return true;
    }

    /**
     * Call stored procedure
     *
     * @param query
     * @param reader
     * @return
     */
    public static boolean call(String query, ReadStH reader) {
        return call(query, reader, null);
    }

    /**
     * Call stored procedure
     *
     * @param query
     * @param reader
     * @param errMsg
     * @return
     */
    public static boolean call(String query, ReadStH reader, String errMsg) {
        Connection con = null;
        CallableStatement stmt = null;
        ResultSet rset;
        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareCall(query);
            if (reader instanceof CallReadStH) {
                ((CallReadStH) reader).setParams(stmt);
            }
            rset = stmt.executeQuery();
            reader.handleRead(rset);
        } catch (Exception e) {
            if (errMsg == null) {
                log.warn("Error calling stored procedure " + e, e);
            } else {
                log.warn(errMsg + " " + e, e);
            }
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                log.warn("Failed to close DB connection " + e, e);
            }
        }
        return true;
    }

    /**
     * Executes Insert or Update Query not needing any further modification or
     * batching. Recycles connection after completion.
     *
     * @param query
     * @return boolean Success
     */
    public static boolean insertUpdate(String query) {
        return insertUpdate(query, null, null);
    }

    /**
     * Executes Insert or Update Query not needing any further modification or
     * batching. Recycles connection after completion.
     *
     * @param query
     * @param errMsg
     * @return success
     */
    public static boolean insertUpdate(String query, String errMsg) {
        return insertUpdate(query, null, errMsg);
    }

    /**
     * Executes Insert / Update Query. Utilizes IUSth for Batching and Query
     * Editing. MUST MANUALLY EXECUTE QUERY / BATACH IN IUSth (No need to close
     * Statement after execution)
     *
     * @param query
     * @param batch
     * @return boolean Success
     */
    public static boolean insertUpdate(String query, IUStH batch) {
        return insertUpdate(query, batch, null);
    }

    /**
     * Executes Insert or Update Query. Utilizes IUSth for Batching and Query
     * Editing. Defines custom error message if error occurs. MUST MANUALLY
     * EXECUTE QUERY / BATACH IN IUSth (No need to Statement after execution)
     * Recycles connection after completion
     *
     * @param query
     * @param batch
     * @param errMsg
     * @return boolean Success
     */
    public static boolean insertUpdate(String query, IUStH batch, String errMsg) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareStatement(query);
            if (batch != null) {
                batch.handleInsertUpdate(stmt);
            } else {
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            if (errMsg == null) {
                log.warn("Failed to execute IU query " + e, e);
            } else {
                log.warn(errMsg + " " + e, e);
            }
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                log.warn("Failed to close DB connection " + e, e);
            }
        }
        return true;
    }

    /**
     * Begins new transaction
     *
     * @return new Transaction object
     * @throws java.sql.SQLException if was unable to create transaction
     */
    public static Transaction beginTransaction() throws SQLException {
        Connection con = DatabaseFactory.getConnection();
        return new Transaction(con);
    }

    /**
     * Creates PreparedStatement with given sql string.<br>
     * Statemens are created with {@link java.sql.ResultSet#TYPE_FORWARD_ONLY}
     * and {@link java.sql.ResultSet#CONCUR_READ_ONLY}
     *
     * @param sql SQL querry
     * @return Prepared statement if ok or null if error happend while creating
     */
    public static PreparedStatement prepareStatement(String sql) {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * Creates {@link java.sql.PreparedStatement} with given sql<br>
     *
     * @param sql SQL querry
     * @param resultSetType a result set type; one of <br>
     * <code>ResultSet.TYPE_FORWARD_ONLY</code>,<br>
     * <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <br>
     * <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency a concurrency type; one of <br>
     * <code>ResultSet.CONCUR_READ_ONLY</code> or <br>
     * <code>ResultSet.CONCUR_UPDATABLE</code>
     * @return Prepared Statement if ok or null if error happened while creating
     */
    public static PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = DatabaseFactory.getConnection();
            ps = c.prepareStatement(sql, resultSetType, resultSetConcurrency);
        } catch (Exception e) {
            log.error("Can't create PreparedStatement for querry: " + sql, e);
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e1) {
                    log.error("Can't close connection after exception", e1);
                }
            }
        }

        return ps;
    }

    /**
     * Executes PreparedStatement
     *
     * @param statement PreparedStatement to execute
     * @return returns result of
     * {@link java.sql.PreparedStatement#executeQuery()} or -1 in case of error
     */
    public static int executeUpdate(PreparedStatement statement) {
        try {
            return statement.executeUpdate();
        } catch (Exception e) {
            log.error("Can't execute update for PreparedStatement", e);
        }

        return -1;
    }

    /**
     * Executes PreparedStatement and closes it and it's connection
     *
     * @param statement PreparedStatement to close
     */
    public static void executeUpdateAndClose(PreparedStatement statement) {
        executeUpdate(statement);
        close(statement);
    }

    /**
     * Executes Querry and returns ResultSet
     *
     * @param statement preparedStement to execute
     * @return ResultSet or null if error
     */
    public static ResultSet executeQuerry(PreparedStatement statement) {
        ResultSet rs = null;
        try {
            rs = statement.executeQuery();
        } catch (Exception e) {
            log.error("Error while executing querry", e);
        }
        return rs;
    }

    /**
     * Closes PreparedStatemet, it's connection and last ResultSet
     *
     * @param statement statement to close
     */
    public static void close(PreparedStatement statement) {
        try {
            if (statement.isClosed()) {
                // noinspection ThrowableInstanceNeverThrown
                log.warn("Attempt to close PreparedStatement that is closes already", new Exception());
                return;
            }
            Connection c = statement.getConnection();
            statement.close();
            c.close();
        } catch (Exception e) {
            log.error("Error while closing PreparedStatement", e);
        }
    }
}