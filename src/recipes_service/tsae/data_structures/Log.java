/*
 * Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of the practical assignment of Distributed Systems course.
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this code.  If not, see <http://www.gnu.org/licenses/>.
 */

package recipes_service.tsae.data_structures;

import edu.uoc.dpcs.lsim.logger.LoggerManager;
import lsim.library.api.LSimLogger;
import recipes_service.data.Operation;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 */
public class Log implements Serializable {
    // Only for the zip file with the correct solution of phase1.Needed for the logging system for the phase1. sgeag_2018p
//	private transient LSimCoordinator lsim = LSimFactory.getCoordinatorInstance();
    // Needed for the logging system sgeag@2017
//	private transient LSimWorker lsim = LSimFactory.getWorkerInstance();

    private static final long serialVersionUID = -4864990265268259700L;
    /**
     * This class implements a log, that stores the operations
     * received  by a client.
     * They are stored in a ConcurrentHashMap (a hash table),
     * that stores a list of operations for each member of
     * the group.
     */
    private final ConcurrentHashMap<String, List<Operation>> log = new ConcurrentHashMap<String, List<Operation>>();

    public Log(List<String> participants) {
        // create an empty log
        for (Iterator<String> it = participants.iterator(); it.hasNext(); ) {
            log.put(it.next(), new Vector<Operation>());
        }
    }

    /**
     * inserts an operation into the log. Operations are
     * inserted in order. If the last operation for
     * the user is not the previous operation than the one
     * being inserted, the insertion will fail.
     *
     * @param newOp
     * @return true if op is inserted, false otherwise.
     */
    public synchronized boolean add(Operation newOp) {
        String hostId = newOp.getTimestamp().getHostid();
        List<Operation> operations = log.get(hostId);
        // if there is no operation for that host id, it is just directly added
        if (operations == null || operations.isEmpty()) {
            log.put(hostId, new Vector<Operation>());
            return log.get(hostId).add(newOp);
        } else {
            // access to the last user operation
            int indexLastOp = operations.size() - 1;
            Operation lastOp = operations.get(indexLastOp);
            // if newOp is newer then is stored otherwise is ignored
            if (newOp.getTimestamp().compare(lastOp.getTimestamp()) > 0) {
                return log.get(hostId).add(newOp);
            }
        }
        return false;
    }

    /**
     * Checks the received summary (otherSummaryVector) and determines the operations
     * contained in the log that have not been seen by
     * the proprietary of the summary.
     * Returns them in an ordered list.
     *
     * @param otherSummaryVector
     * @return list of operations
     */
    public synchronized List<Operation> listNewer(TimestampVector otherSummaryVector) {
        List<Operation> pendingOperations = new Vector<>();
        for (Map.Entry<String, List<Operation>> entry : log.entrySet()) {
            String hostId = entry.getKey();
            Timestamp otherLastSummaryTimestamp = otherSummaryVector.getLast(hostId);
            if (otherLastSummaryTimestamp != null) {
                List<Operation> currentLogOperationsByHost = entry.getValue();
                for (Operation currentOperation : currentLogOperationsByHost) {
                    // determines if the current operation is pending to be seen for the other node
                    long comparison = currentOperation.getTimestamp().compare(otherLastSummaryTimestamp);
                    if (comparison > 0) {
                        pendingOperations.add(currentOperation);
                    }
                }
            }
        }
        return pendingOperations;
    }

    /**
     * Removes from the log the operations that have
     * been acknowledged by all the members
     * of the group, according to the provided
     * ackSummary.
     *
     * @param ack: ackSummary.
     */
    public synchronized void purgeLog(TimestampMatrix ack) {
        LSimLogger.log(LoggerManager.Level.TRACE, "PURGING LOG");
        List<Operation> messages;
        TimestampVector minTimestampVector = ack.minTimestampVector();
        LSimLogger.log(LoggerManager.Level.TRACE, "minTimestampVector " + minTimestampVector);
        for (Map.Entry<String, List<Operation>> entry : log.entrySet()) {
            String hostId = entry.getKey();
            Timestamp lastAck = minTimestampVector.getLast(hostId);
            if (!lastAck.isNullTimestamp()) {
                int index = 0;
                messages = entry.getValue();
                while (index < messages.size()) {
                    Operation op = messages.get(index);
                    if (op.getTimestamp().compare(lastAck) <= 0) {
                        LSimLogger.log(LoggerManager.Level.TRACE, "REMOVING OPERATION " + op);
                        messages.remove(index);
                    } else {
                        index++;
                    }
                }
            }
        }
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Log)) return false;
        Log log1 = (Log) o;
        return log.equals(log1.log);
    }

    /**
     * toString
     */
    @Override
    public synchronized String toString() {
        String name = "";
        for (Enumeration<List<Operation>> en = log.elements();
             en.hasMoreElements(); ) {
            List<Operation> sublog = en.nextElement();
            for (ListIterator<Operation> en2 = sublog.listIterator(); en2.hasNext(); ) {
                name += en2.next().toString() + "\n";
            }
        }

        return name;
    }
}
