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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.whirr.cm.server.CmServer;

public abstract class CmServerLog {

  protected String tag;
  protected boolean quiet;

  public static abstract class CmServerLogSyncCommand {
    public abstract void execute() throws Exception;
  }

  public CmServerLog() {
    this(null, false);
  }

  public CmServerLog(boolean quiet) {
    this(null, quiet);
  }

  public CmServerLog(String tag, boolean quiet) {
    this.tag = tag;
    this.quiet = quiet;
  }

  public void logSpacer() {
    logOperation("", "");
  }

  public void logSpacerDashed() {
    logOperation("", "-----------------------------------------------------------------");
  }

  public void logOperation(String message) {
    logOperation(null, message);
  }

  public void logOperation(String operation, String message) {
    if (!quiet) {
      logMessage((operation == null ? "" : (tag == null ? "" : (tag + " ")))
          + (operation == null || operation.equals("") ? "" : (((tag == null ? "" : "[") + operation
              + (tag == null ? "" : "]") + " "))) + (message == null ? "" : message));
    }
  }

  public void logOperationIntermediate(String message) {
    logOperationIntermediate(null, message);
  }

  public void logOperationIntermediate(String operation, String message) {
    if (!quiet) {
      logMessageIntermediate((operation == null ? "" : (tag == null ? "" : (tag + " ")))
          + (operation == null || operation.equals("") ? "" : (((tag == null ? "" : "[") + operation
              + (tag == null ? "" : "]") + " "))) + (message == null ? "" : message));
    }
  }

  public void logOperationStackTrace(Throwable throwable) {
    logOperationStackTrace(null, throwable);
  }

  public void logOperationStackTrace(String operation, Throwable throwable) {
    if (!quiet) {
      StringWriter stringWriter = new StringWriter();
      throwable.printStackTrace(new PrintWriter(stringWriter));
      for (String stackTraceLine : stringWriter.toString().split(System.getProperty("line.separator"))) {
        logOperation(operation, stackTraceLine);
      }
    }
  }

  protected abstract void logMessage(String message);

  protected abstract void logMessageIntermediate(String message);

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

    public CmServerLogNull() {
      super();
    }

    public CmServerLogNull(boolean quiet) {
      super(null, quiet);
    }

    public CmServerLogNull(String tag, boolean quiet) {
      super(tag, quiet);
    }

    @Override
    protected void logMessage(String message) {
    }

    @Override
    protected void logMessageIntermediate(String message) {
    }

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

    private static Logger logOperation = LoggerFactory.getLogger(CmServer.class);

    public CmServerLogSlf4j() {
      super();
    }

    public CmServerLogSlf4j(boolean quiet) {
      super(null, quiet);
    }

    public CmServerLogSlf4j(String tag, boolean quiet) {
      super(tag, quiet);
    }

    @Override
    protected void logMessage(String message) {
      if (logOperation.isInfoEnabled()) {
        logOperation.info(message);
      }
    }

    @Override
    protected void logMessageIntermediate(String message) {
      logMessage(message);
    }

    @Override
    public void logOperation(String operation, CmServerLogSyncCommand command) {
      boolean failed = false;
      logOperation(operation, "started");
      try {
        command.execute();
      } catch (Exception e) {
        failed = true;
        logOperation(operation, "Unexpected error executing command");
      }
      logOperation(operation, (failed ? "failed" : "finished"));
    }

    @Override
    public void logOperationStartedAsync(String operation) {
      logOperation(operation, "started");
    }

    @Override
    public void logOperationInProgressAsync(String operation) {
      logOperation(operation, "in progress");
    }

    @Override
    public void logOperationFailedAsync(String operation) {
      logOperation(operation, "failed");
    }

    @Override
    public void logOperationFailedAsync(String operation, Throwable throwable) {
      logOperation(operation, "failed");
      logOperationStackTrace(operation, throwable);
    }

    @Override
    public void logOperationFinishedAsync(String operation) {
      logOperation(operation, "finished");
    }

    @Override
    public void logOperationStartedSync(String operation) {
      logOperationStartedAsync(operation);
    }

    @Override
    public void logOperationInProgressSync(String operation, String detail) {
      logOperation(operation, detail);
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

    public CmServerLogSysOut(String tag, boolean quiet) {
      super(tag, quiet);
    }

    public CmServerLogSysOut() {
      super();
    }

    public CmServerLogSysOut(boolean quiet) {
      super(null, quiet);
    }

    @Override
    protected void logMessage(String message) {
      System.out.println(message);
    }

    @Override
    public void logMessageIntermediate(String message) {
      System.out.print(message);
    }

    @Override
    public void logOperation(String operation, CmServerLogSyncCommand command) {
      boolean failed = false;
      logOperationIntermediate(operation, "started .");
      try {
        command.execute();
      } catch (Exception e) {
        failed = true;
        logOperation(".. failed");
        e.printStackTrace();
      }
      if (!failed) {
        logOperation(".. finished");
      }
    }

    @Override
    public void logOperationStartedAsync(String operation) {
      logOperationIntermediate(operation, "started .");
    }

    @Override
    public void logOperationInProgressAsync(String operation) {
      logOperationIntermediate(".");
    }

    @Override
    public void logOperationFailedAsync(String operation) {
      logOperation(".. failed");
    }

    @Override
    public void logOperationFailedAsync(String operation, Throwable throwable) {
      logOperation(".. failed");
      logOperationStackTrace(operation, throwable);
    }

    @Override
    public void logOperationFinishedAsync(String operation) {
      logOperation(".. finished");
    }

    @Override
    public void logOperationStartedSync(String operation) {
      logOperation(operation, "started");
    }

    @Override
    public void logOperationInProgressSync(String operation, String detail) {
      logOperation(operation, detail);
    }

    @Override
    public void logOperationFailedSync(String operation) {
      logOperation(operation, "failed");
    }

    @Override
    public void logOperationFailedSync(String operation, Throwable throwable) {
      logOperation(operation, "failed");
      logOperationStackTrace(operation, throwable);
    }

    @Override
    public void logOperationFinishedSync(String operation) {
      logOperation(operation, "finished");
    }

  }

}
