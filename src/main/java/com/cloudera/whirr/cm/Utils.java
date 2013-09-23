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
package com.cloudera.whirr.cm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class Utils {

  public static URL urlForURI(String inputStr) {
    try {
      if (inputStr == null) {
        return null;
      }
      URI input = new URI(inputStr).normalize();
      if (input.getScheme() != null && input.getScheme().equals("classpath")) {
        return Utils.class.getResource(input.getPath());
      }
      if (Resources.toString(input.toURL(), Charsets.UTF_8) != null) {
        return input.toURL();
      } else {
        return null;
      }
    } catch (IOException e) {
      // Swallow the exception for now and just return null - this could probably be better.
      return null;
    } catch (URISyntaxException e) {
      // Swallow the exception for now and just return null - this could probably be better.
      return null;
    }
  }
}
