package com.cloudera.whirr.cm.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CmServerSmokeSuiteIT extends CmServerSmokeIT {

  private static String[] OS_VERSION_MATRIX = new String[] { "centos", "ubuntu" };
  private static String[][] CM_VERSION_MATRIX = new String[][] { { "cm4.5.0", "v3", "cdh4" },
      { "cm4.6.0", "v3", "cdh4" }, { "cm4.6.0", "v4", "cdh4" }, { "cm4.7.0", "v4", "cdh4" }, { "", "", "" } };

  @Parameters
  public static Collection<String[]> data() {
    List<String[]> records = new ArrayList<String[]>();
    for (int i = 0; i < OS_VERSION_MATRIX.length; i++) {
      for (int j = 0; j < CM_VERSION_MATRIX.length; j++) {
        List<String> record = new ArrayList<String>();
        record.add(OS_VERSION_MATRIX[i]);
        for (int k = 0; k < CM_VERSION_MATRIX[j].length; k++) {
          record.add(CM_VERSION_MATRIX[j][k]);
        }
        records.add(record.toArray(new String[record.size()]));
      }
    }
    return records;
  }

  public CmServerSmokeSuiteIT(String platform, String cm, String api, String cdh) {
    this.platform = platform;
    this.cm = cm;
    this.api = api;
    this.cdh = cdh;
  }

}