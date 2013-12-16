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
package org.jboss.aerogear.android.impl.pipeline;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.Pipeline;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.RecordId;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.helper.UnitTestUtils;
import org.jboss.aerogear.android.impl.http.HttpStubProvider;
import org.jboss.aerogear.android.pipeline.Pipe;
import org.junit.Assert;
import org.junit.Before;

public class MultipartRequestBuilderTest {

    private static final String STRING_DATA = "This is a String";
    public Queue<String> multiPartSerializedData;

    @Before
    public void resetSeriaializedData() {
        multiPartSerializedData = new ArrayDeque<String>(20);
        multiPartSerializedData.add("multipart/form-data; boundary=[\\w\\d-]+");

        multiPartSerializedData.add("--[\\w\\d-]+\r\n");
        multiPartSerializedData.add("Content-Disposition: form-data; name=\"string\"\r\n");
        multiPartSerializedData.add("Content-Type: text/plain; charset=US-ASCII\r\n");
        multiPartSerializedData.add("Content-Transfer-Encoding: 8bit\r\n");
        multiPartSerializedData.add("\r\n");
        multiPartSerializedData.add(STRING_DATA + "\r\n");
        multiPartSerializedData.add("--[\\w\\d-]+\r\n");
        multiPartSerializedData.add("Content-Disposition: form-data; name=\"files\"\r\n");
        multiPartSerializedData.add("Content-Type: multipart/mixed; boundary=[\\w\\d-]+\r\n");
        multiPartSerializedData.add("\r\n");
        multiPartSerializedData.add("--[\\w\\d-]+\r\n");
        multiPartSerializedData.add("Content-Disposition: file; filename=\"inputStream\"\r\n");
        multiPartSerializedData.add("Content-Type: application/octet-stream\r\n");
        multiPartSerializedData.add("Content-Transfer-Encoding: binary\r\n");
        multiPartSerializedData.add("\r\n");
        multiPartSerializedData.add("abcdef\r\n");
        multiPartSerializedData.add("--[\\w\\d-]+\r\n");
        multiPartSerializedData.add("Content-Disposition: file; filename=\"byteArray\"\r\n");
        multiPartSerializedData.add("Content-Type: application/octet-stream\r\n");
        multiPartSerializedData.add("Content-Transfer-Encoding: binary\r\n");
        multiPartSerializedData.add("\r\n");
        multiPartSerializedData.add("abcdef\r\n");
        multiPartSerializedData.add("--[\\w\\d-]+--\r\n");
        multiPartSerializedData.add("--[\\w\\d-]+--\r\n");
    }

    @Test
    public void testGetContentType() {
        MultipartRequestBuilder<Object> builder = new MultipartRequestBuilder();
        assertTrue(builder.getContentType().startsWith("multipart/form-data;"));
    }

    @Test
    public void testBuilder() {

        MultiPartData data = new MultiPartData();
        MultipartRequestBuilder<MultiPartData> dataBuilder = new MultipartRequestBuilder<MultiPartData>();
        assertTrue(dataBuilder.getContentType().matches(multiPartSerializedData.poll()));
        byte[] out = dataBuilder.getBody(data);
        ByteArrayInputStream stream = new ByteArrayInputStream(out);
        StringBuilder stringBuilder = new StringBuilder();
        int readByte;
        while ((readByte = stream.read()) != -1) {
            stringBuilder.append((char) readByte);
            if (((char) readByte) == '\n') {
                assertTrue(stringBuilder.toString().matches(multiPartSerializedData.poll()));
                stringBuilder = new StringBuilder();
            }
        }

    }

    @Test
    public void testMultipartBuilderCall() throws MalformedURLException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException {
        URL url = new URL("http://example.com");
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        final HttpStubProvider provider = new HttpStubProvider(url, null) {

            @Override
            public HeaderAndBody put(String id, byte[] data) throws HttpException {
                byteBuffer.put(data);
                return new HeaderAndBody("{}".getBytes(), new HashMap<String, Object>());
            }
        };

        PipeConfig config = new PipeConfig(url, MultipartRequestBuilderTest.MultiPartData.class);
        config.setRequestBuilder(new MultipartRequestBuilder<MultipartRequestBuilderTest.MultiPartData>());

        Pipeline pipeline = new Pipeline(url);
        Pipe<MultipartRequestBuilderTest.MultiPartData> restPipe = pipeline.pipe(MultipartRequestBuilderTest.MultiPartData.class, config);
        org.junit.Assert.assertEquals(MultipartRequestBuilder.class, restPipe.getRequestBuilder().getClass());

        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);

        restPipe.save(new MultipartRequestBuilderTest.MultiPartData(), new Callback<MultipartRequestBuilderTest.MultiPartData>() {
            @Override
            public void onSuccess(MultipartRequestBuilderTest.MultiPartData data) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                Logger.getLogger(MultiPartData.class.getCanonicalName()).log(Level.SEVERE, e.getMessage(), e);
                latch.countDown();
            }
        });

        latch.await();

        //move data past the header
        multiPartSerializedData.poll();

        ByteArrayInputStream stream = new ByteArrayInputStream(byteBuffer.array());
        StringBuilder stringBuilder = new StringBuilder();
        int readByte;
        while ((readByte = stream.read()) != -1) {
            stringBuilder.append((char) readByte);
            if (((char) readByte) == '\n') {
                assertTrue(stringBuilder.toString().matches(multiPartSerializedData.poll()));
                stringBuilder = new StringBuilder();
            }
        }

    }

    @Test
    public void testBuilderUsage() throws MalformedURLException {
        URL baseUrl = new URL("http://example.com");
        PipeConfig config = new PipeConfig(baseUrl, MultiPartData.class);
        config.setRequestBuilder(new MultipartRequestBuilder<MultiPartData>());

        Pipeline pipeline = new Pipeline(baseUrl);
        Pipe<MultiPartData> pipe = pipeline.pipe(MultiPartData.class, config);
        Assert.assertEquals(MultipartRequestBuilder.class, pipe.getRequestBuilder().getClass());

    }

    public static class MultiPartData {

        private byte[] byteArray = { 'a', 'b', 'c', 'd', 'e', 'f' };
        private InputStream inputStream = new ByteArrayInputStream(byteArray);

        @RecordId
        private String string = STRING_DATA;

        public byte[] getByteArray() {
            return byteArray;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getString() {
            return string;
        }

        public void setByteArray(byte[] byteArray) {
            this.byteArray = byteArray;
        }

        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void setString(String string) {
            this.string = string;
        }
    }
}
