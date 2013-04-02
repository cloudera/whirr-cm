/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.whirr.cm.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.whirr.cm.server.CmServer;

public abstract class CmServerLog {

  private static Logger logOperation = LoggerFactory.getLogger(CmServer.class);

  private static final String LOG_REFIX = "CM Server";

  public static abstract class CmServerLogSyncCommand {
    public abstract void execute() throws Exception;
  }

  public abstract void log(String message);

  public abstract void logOperation(String operation, CmServerLogSyncCommand command);

  public abstract void logOperationStartedSync(String operation);

  public abstract void logOperationInProgressSync(String operation, String detail);

  public abstract void logOperationFailedSync(String operation);

  public abstract void logOperationFailedSync(String operation, Throwable throwable);

  public abstract void logOperationFinishedSync(String operation);

  public abstract void logOperationStartedAsync(String operation);

  public abstract void logOperationInProgressAsync(String operation);

  public abstract void logOperationFailedAsync(String operation);

  public abstract void logOperationFailedAsync(String operation, Throwable throwable);

  public abstract void logOperationFinishedAsync(String operation);

  public static class CmServerLogNull extends CmServerLog {

    @Override
    public void logOperation(String operation, CmServerLogSyncCommand command) {
      try {
        command.execute();
      } catch (Exception e) {
        // ignore
      }
    }

    @Override
    public void logOperationStartedAsync(String operation) {
    }

    @Override
    public void logOperationInProgressAsync(String operation) {
    }

    @Override
    public void logOperationFailedAsync(String operation) {
    }

    @Override
    public void logOperationFailedAsync(String operation, Throwable throwable) {
    }

    @Override
    public void logOperationFinishedAsync(String operation) {
    }

    @Override
    public void log(String message) {
    }

    @Override
    public void logOperationStartedSync(String operation) {
    }

    @Override
    public void logOperationInProgressSync(String operation, String detail) {
    }

    @Override
    public void logOperationFailedSync(String operation) {
    }

    @Override
    public void logOperationFailedSync(String operation, Throwable throwable) {
    }

    @Override
    public void logOperationFinishedSync(String operation) {
    }

  }

  public static class CmServerLogSlf4j extends CmServerLog {

    @Override
    public void logOperation(String operation, CmServerLogSyncCommand command) {
      boolean failed = false;
      log(LOG_REFIX + " [" + operation + "] started");
      try {
        command.execute();
      } catch (Exception e) {
        failed = true;
        if (logOperation.isErrorEnabled()) {
          logOperation.error("Unexpected error executing command", e);
        }
      }
      log(LOG_REFIX + " [" + operation + "] " + (failed ? "failed" : "finished"));
    }

    @Override
    public void logOperationStartedAsync(String operation) {
      log(LOG_REFIX + " [" + operation + "] started");
    }

    @Override
    public void logOperationInProgressAsync(String operation) {
      log(LOG_REFIX + " [" + operation + "] in progress");
    }

    @Override
    public void logOperationFailedAsync(String operation) {
      log(LOG_REFIX + " [" + operation + "] failed");
    }

    @Override
    public void logOperationFailedAsync(String operation, Throwable throwable) {
      log(LOG_REFIX + " [" + operation + "] failed", throwable);
    }

    @Override
    public void logOperationFinishedAsync(String operation) {
      log(LOG_REFIX + " [" + operation + "] finished");
    }

    @Override
    public void log(String message) {
      if (logOperation.isInfoEnabled()) {
        logOperation.info(message);
      }
    }

    public void log(String message, Throwable throwable) {
      if (logOperation.isInfoEnabled()) {
        logOperation.info(message, throwable);
      }
    }

    @Override
    public void logOperationStartedSync(String operation) {
      logOperationStartedAsync(operation);
    }

    @Override
    public void logOperationInProgressSync(String operation, String detail) {
      log(LOG_REFIX + " [" + operation + "] " + detail);
    }

    @Override
    public void logOperationFailedSync(String operation) {
      logOperationFailedAsync(operation);
    }

    @Override
    public void logOperationFailedSync(String operation, Throwable throwable) {
      logOperationFailedAsync(operation, throwable);
    }

    @Override
    public void logOperationFinishedSync(String operation) {
      logOperationFinishedAsync(operation);
    }

  }

  public static class CmServerLogSysOut extends CmServerLog {

    @Override
    public void logOperation(String operation, CmServerLogSyncCommand command) {
      boolean failed = false;
      System.out.print(LOG_REFIX + " [" + operation + "] started .");
      try {
        command.execute();
      } catch (Exception e) {
        failed = true;
        System.out.println(" . failed");
        e.printStackTrace();
      }
      if (!failed) {
        System.out.println(" . finished");
      }
    }

    @Override
    public void logOperationStartedAsync(String operation) {
      System.out.print(LOG_REFIX + " [" + operation + "] started");
    }

    @Override
    public void logOperationInProgressAsync(String operation) {
      System.out.print(" .");
    }

    @Override
    public void logOperationFailedAsync(String operation) {
      System.out.println(" . failed");
    }

    @Override
    public void logOperationFailedAsync(String operation, Throwable throwable) {
      System.out.println(" . failed");
      throwable.printStackTrace(System.out);
    }

    @Override
    public void logOperationFinishedAsync(String operation) {
      System.out.println(" . finished");
    }

    @Override
    public void log(String message) {
      System.out.println(message);
    }

    @Override
    public void logOperationStartedSync(String operation) {
      System.out.println(LOG_REFIX + " [" + operation + "] started");
    }

    @Override
    public void logOperationInProgressSync(String operation, String detail) {
      System.out.println(LOG_REFIX + " [" + operation + "] " + detail);
    }

    @Override
    public void logOperationFailedSync(String operation) {
      System.out.println(LOG_REFIX + " [" + operation + "] failed");
    }

    @Override
    public void logOperationFailedSync(String operation, Throwable throwable) {
      System.out.println(LOG_REFIX + " [" + operation + "] failed");
      throwable.printStackTrace(System.out);
    }

    @Override
    public void logOperationFinishedSync(String operation) {
      System.out.println(LOG_REFIX + " [" + operation + "] finished");
    }

  }

}
