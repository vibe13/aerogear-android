/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.aerogear.android.pipeline;

import java.nio.charset.Charset;

/**
 * This class holds configuration data used by requestBuilders and response 
 * parsers to manage data.
 */
public class MarshallingConfig {

    private Charset encoding = Charset.forName("UTF-8");
    private String dataRoot = "";

    /**
     * Encoding is the data encoding of the http body.
     *
     * Default is "UTF-8"
     *
     * @return the current encoding.
     */
    public Charset getEncoding() {
        return encoding;
    }

    /**
     * Encoding is the data encoding of the http body.
     *
     * Default is "UTF-8"
     *
     * @param encoding a new encoding to set
     */
    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    /**
     * See: {@link PipeConfig#getDataRoot() }
     *
     * @return the current DataRoot
     */
    public String getDataRoot() {
        return dataRoot;
    }

    /**
     * See: {@link PipeConfig#getDataRoot() }
     *
     * @return the current DataRoot
     */
    public void setDataRoot(String dataRoot) {
        this.dataRoot = dataRoot;
    }
}
