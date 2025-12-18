package org.apache.torque.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This is a utility class which eases getting average of a column with criteria
 *
 * @author <a href="mailto:jeff@jivecast.com">Jeffery Painter</a>
 * @author <a href="mailto:Martin.Goulet@sungard.com">Martin Goulet</a>
 * @author <a href="mailto:eric.lambert@sungard.com">Eric Lambert</a>
 * @author <a href="mailto:sebastien.paquette@sungard.com">Sebastien Paquette</a>
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */


import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import org.apache.torque.Column;
import org.apache.torque.ColumnImpl;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.map.TableMap;
import org.apache.torque.om.mapper.BigDecimalMapper;
import org.apache.torque.util.functions.Avg;

/**
 * Get's the average of a column with entries matching the provided criteria.
 * This works similarly to the CountHelper when you need to provide
 * additional selection criteria to compute an average. 
 * 
 * For example, limiting the average of a column in a table to a specific user.
 */
public class AvgHelper
{

    /**
     * Returns the average of a column in a query.
     *
     * @param c Criteria to get the count for.
     * @param columnName Name of database Column which is counted. Preferably,
     *        use the primary key here.
     * @return average of the column matching the query provided
     * @throws TorqueException if the query could not be executed
     */
    public BigDecimal avg(final Criteria c, final String columnName)
            throws TorqueException
    {
        return avg(c, null, columnName, null);
    }

    /**
     * Returns the average of a column in a query.
     *
     * @param c Criteria to get the count for.
     * @param columnName Name of database Column which is averaged.
     * 
     * @return average of the column matching the query provided
     * @throws TorqueException if the query could not be executed
     */
    public BigDecimal avg(final Criteria c, final Column column)
            throws TorqueException
    {
        return avg(c, column.getSqlExpression());
    }

    /**
     * Returns the average of a column in a query.
     *
     * @param c Criteria to get the count for.
     * @param conn Connection to use
     * @param columnName Name of database Column which is averaged.
     * @return average of the column matching the query provided
     * @throws TorqueException if the query could not be executed
     */
    public BigDecimal avg(
            final Criteria c,
            final Connection conn,
            final Column column)
                    throws TorqueException
    {
        return avg(c, conn, column.getSqlExpression(), null);
    }


    /**
     * Returns the average of a column in a query.
     *
     * @param c Criteria to get the count for.
     * @param conn Connection to use
     * @param columnName Name of database Column which is averaged.
     * @param tableMap the table to count the columns in, or null to determine
     *        the table automatically from the criteria.
     *
     * @return average of the column matching the query provided
     *
     * @throws TorqueException if the query could not be executed.
     */
    public BigDecimal avg(
            final Criteria c,
            final Connection conn,
            final String columnName,
            final TableMap tableMap)
                    throws TorqueException
    {
        /* Clear the select columns. */
        c.getSelectColumns().clear();
        c.getOrderByColumns().clear();
        c.getGroupByColumns().clear();

        UniqueList<String> criteriaSelectModifiers
        = c.getSelectModifiers();

        boolean distinct = false;
        if (criteriaSelectModifiers != null
                && criteriaSelectModifiers.size() > 0
                && criteriaSelectModifiers.contains(SqlEnum.DISTINCT.toString()))
        {
            criteriaSelectModifiers.remove(SqlEnum.DISTINCT.toString());
            distinct = true;
        }

        c.addSelectColumn(new Avg(new ColumnImpl(columnName), distinct));

        String databaseName = (c.getDbName() == null)
                ? Torque.getDefaultDB()
                        : c.getDbName();

                BasePeerImpl<BigDecimal> peer = new BasePeerImpl<>(
                        new BigDecimalMapper(),
                        tableMap, databaseName);

                List<BigDecimal> result = (conn == null)
                        ? peer.doSelect(c)
                                : peer.doSelect(c, conn);

                        return result.get(0);
    }
}