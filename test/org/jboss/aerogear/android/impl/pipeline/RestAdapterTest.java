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

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import junit.framework.Assert;
import org.jboss.aerogear.android.*;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.core.HttpProviderFactory;
import org.jboss.aerogear.android.impl.helper.Data;
import org.jboss.aerogear.android.impl.helper.UnitTestUtils;
import org.jboss.aerogear.android.impl.http.HttpStubProvider;
import org.jboss.aerogear.android.impl.pipeline.paging.WrappingPagedList;
import org.jboss.aerogear.android.pipeline.Pipe;
import org.jboss.aerogear.android.pipeline.paging.PageConfig;
import org.jboss.aerogear.android.pipeline.paging.PagedList;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RestAdapterTest {

    private static final String TAG = RestAdapterTest.class.getSimpleName();
    private static final String SERIALIZED_POINTS = "{\"points\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":2},{\"x\":2,\"y\":4},{\"x\":3,\"y\":6},{\"x\":4,\"y\":8},{\"x\":5,\"y\":10},{\"x\":6,\"y\":12},{\"x\":7,\"y\":14},{\"x\":8,\"y\":16},{\"x\":9,\"y\":18}],\"id\":\"1\"}";
    private static final String POINTS_ARRAY = "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":2},{\"x\":2,\"y\":4},{\"x\":3,\"y\":6},{\"x\":4,\"y\":8},{\"x\":5,\"y\":10},{\"x\":6,\"y\":12},{\"x\":7,\"y\":14},{\"x\":8,\"y\":16},{\"x\":9,\"y\":18}]";
    private URL url;
    private URL urlNoSlash;
    private final Provider<HttpProvider> stubHttpProviderFactory = new Provider<HttpProvider>() {
        @Override
        public HttpProvider get(Object... in) {
            return new HttpStubProvider((URL) in[0]);
        }
    };

    @Before
    public void setup() throws MalformedURLException {
        url = new URL("http://server.com/context/");
        urlNoSlash = new URL("http://server.com/context");
    }

    @Test
    public void testPipeTypeProperty() throws Exception {
        Pipe<Data> restPipe = new RestAdapter<Data>(Data.class, url);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", stubHttpProviderFactory);
        Assert.assertEquals("verifying the (default) type", PipeTypes.REST, restPipe.getType());
    }

    @Test
    public void testPipeURLProperty() throws Exception {
        Pipe<Data> restPipe = new RestAdapter<Data>(Data.class, url);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", stubHttpProviderFactory);
        assertEquals("verifying the given URL", "http://server.com/context/", restPipe.getUrl().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeFactoryPipeConfigEncoding() {
        PipeConfig config = new PipeConfig(url, Data.class);
        config.setEncoding("UTF-16");
        assertEquals(Charset.forName("UTF-16"), config.getEncoding());
        config.setEncoding((Charset) null);
    }

    @Test
    public void testPipeFactoryPipeConfigGson() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());

        DefaultPipeFactory factory = new DefaultPipeFactory();
        PipeConfig pc = new PipeConfig(url, ListClassId.class);

        GsonResponseParser<ListClassId> responseParser = new GsonResponseParser<ListClassId>(builder.create());
        pc.setResponseParser(responseParser);

        Pipe<ListClassId> restPipe = factory.createPipe(ListClassId.class, pc);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        Field gsonField = restRunner.getClass().getDeclaredField("responseParser");
        gsonField.setAccessible(true);
        GsonResponseParser gson = (GsonResponseParser) gsonField.get(restRunner);

        assertEquals(responseParser, gson);

    }

    @Test(timeout = 500L)
    public void testEncoding() throws Exception {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());
        final Charset utf_16 = Charset.forName("UTF-16");

        final HttpStubProvider provider = new HttpStubProvider(url, new HeaderAndBody(SERIALIZED_POINTS.getBytes(utf_16), new HashMap<String, Object>()));

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setRequestBuilder(new GsonRequestBuilder(builder.create()));
        config.setEncoding(utf_16);
        RestAdapter<ListClassId> restPipe = new RestAdapter<ListClassId>(ListClassId.class, url, config);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });

        runRead(restPipe);

    }

    @Test(timeout = 500L)
    public void testConfigSetEncoding() throws Exception {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(
                Point.class, new RestAdapterTest.PointTypeAdapter());
        final Charset utf_16 = Charset.forName("UTF-16");

        Pipeline pipeline = new Pipeline(url);
        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setEncoding(utf_16);
        config.setRequestBuilder(new GsonRequestBuilder(builder.create()));

        RestAdapter<ListClassId> restPipe = (RestAdapter<ListClassId>) pipeline
                .pipe(ListClassId.class, config);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        assertEquals(utf_16, UnitTestUtils.getPrivateField(restRunner, "encoding"));

    }

    @Test
    public void testSingleObjectRead() throws Exception {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());
        HeaderAndBody response = new HeaderAndBody(SERIALIZED_POINTS.getBytes(), new HashMap<String, Object>());
        final HttpStubProvider provider = new HttpStubProvider(url, response);

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setRequestBuilder(new GsonRequestBuilder(builder.create()));

        RestAdapter<ListClassId> restPipe = new RestAdapter<ListClassId>(ListClassId.class, url, config);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });
        List<ListClassId> result = runRead(restPipe);

        List<Point> returnedPoints = result.get(0).points;
        assertEquals(10, returnedPoints.size());

    }

    @Test
    public void testSingleObjectReadWithNestedResult() throws Exception {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());
        HeaderAndBody response = new HeaderAndBody(("{\"result\":{\"points\":" + SERIALIZED_POINTS + "}}").getBytes(), new HashMap<String, Object>());
        final HttpStubProvider provider = new HttpStubProvider(url, response);

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setRequestBuilder(new GsonRequestBuilder(builder.create()));
        config.setDataRoot("result.points");

        RestAdapter<ListClassId> restPipe = new RestAdapter<ListClassId>(ListClassId.class, url, config);

        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });
        List<ListClassId> result = runRead(restPipe);

        List<Point> returnedPoints = result.get(0).points;
        assertEquals(10, returnedPoints.size());

    }

    @Test
    public void testReadArray() throws Exception {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());
        HeaderAndBody response = new HeaderAndBody((POINTS_ARRAY).getBytes(), new HashMap<String, Object>());
        final HttpStubProvider provider = new HttpStubProvider(url, response);

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setRequestBuilder(new GsonRequestBuilder(builder.create()));
        config.setDataRoot("");

        RestAdapter<Point> restPipe = new RestAdapter<Point>(Point.class, url, config);

        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });
        List<Point> result = runRead(restPipe);

        assertEquals(10, result.size());

    }

    @Test
    public void testGsonBuilderProperty() throws Exception {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());

        final ByteArrayOutputStream request = new ByteArrayOutputStream();

        final HttpStubProvider provider = new HttpStubProvider(url) {
            @Override
            public HeaderAndBody put(String id, byte[] data) {
                try {
                    request.write(data);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return new HeaderAndBody(data, new HashMap<String, Object>());
            }

            @Override
            public HeaderAndBody post(byte[] data) {
                try {
                    request.write(data);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return new HeaderAndBody(data, new HashMap<String, Object>());
            }
        };

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setResponseParser(new GsonResponseParser(builder.create()));

        Pipe<ListClassId> restPipe = new RestAdapter<ListClassId>(ListClassId.class, url, config);
        Object restRunner = UnitTestUtils.getPrivateField(restPipe, "restRunner");

        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        final ListClassId listClass = new ListClassId(true);
        final List<Point> returnedPoints = new ArrayList<Point>(10);

        restPipe.save(listClass, new Callback<ListClassId>() {
            @Override
            public void onSuccess(ListClassId data) {
                returnedPoints.addAll(data.points);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                throw new RuntimeException(e);
            }
        });

        latch.await(20, TimeUnit.SECONDS);

        assertArrayEquals(SERIALIZED_POINTS.getBytes(), request.toByteArray());
        assertEquals(listClass.points, returnedPoints);
    }

    @Test
    public void runReadWithFilterUsingUriQuery() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        HttpProviderFactory factory = mock(HttpProviderFactory.class);
        when(factory.get(anyObject())).thenReturn(mock(HttpProvider.class));

        RestAdapter<Data> adapter = new RestAdapter<Data>(Data.class, url);
        Object restRunner = UnitTestUtils.getPrivateField(adapter, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", factory);

        ReadFilter filter = new ReadFilter();
        filter.setLinkUri(URI.create("?limit=10&%7B%22model%22:%22BMW%22%7D&token=token"));

        adapter.read(filter, new Callback<List<Data>>() {
            @Override
            public void onSuccess(List<Data> data) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                latch.countDown();
            }
        });
        latch.await(500, TimeUnit.MILLISECONDS);

        verify(factory).get(eq(new URL(urlNoSlash.toString() + "?limit=10&%7B%22model%22:%22BMW%22%7D&token=token")), eq(60000));
    }

    @Test
    public void runReadWithFilterUsingUriPath() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        HttpProviderFactory factory = mock(HttpProviderFactory.class);
        when(factory.get(anyObject())).thenReturn(mock(HttpProvider.class));

        RestAdapter<Data> adapter = new RestAdapter<Data>(Data.class, url);
        Object restRunner = UnitTestUtils.getPrivateField(adapter, "restRunner");
        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", factory);

        ReadFilter filter = new ReadFilter();
        filter.setLinkUri(URI.create("/token"));

        adapter.read(filter, new Callback<List<Data>>() {
            @Override
            public void onSuccess(List<Data> data) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                latch.countDown();
            }
        });
        latch.await(500, TimeUnit.MILLISECONDS);

        verify(factory).get(eq(new URL(urlNoSlash.toString() + "/token")), eq(60000));
    }

    @Test
    public void runReadWithFilterAndAuthentication() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        HttpProviderFactory factory = mock(HttpProviderFactory.class);
        HttpProvider provider = mock(HttpProvider.class);
        HeaderAndBody headerAndBody = mock(HeaderAndBody.class);
        when(headerAndBody.getBody()).thenReturn("".getBytes());
        when(provider.get()).thenReturn(headerAndBody);
        when(factory.get(anyObject(), anyObject())).thenReturn(provider);

        AuthorizationFields authFields = new AuthorizationFields();
        authFields.addQueryParameter("token", "token");

        AuthenticationModule urlModule = mock(AuthenticationModule.class);
        when(urlModule.isLoggedIn()).thenReturn(true);
        when(urlModule.getAuthorizationFields((URI) anyObject(), anyString(), (byte[]) anyObject())).thenReturn(authFields);

        PipeConfig config = new PipeConfig(url, Data.class);
        config.setAuthModule(urlModule);

        RestAdapter<Data> adapter = new RestAdapter<Data>(Data.class, url, config);
        Object restRunner = UnitTestUtils.getPrivateField(adapter, "restRunner");

        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", factory);

        ReadFilter filter = new ReadFilter();
        filter.setLimit(10);
        filter.setWhere(new JSONObject("{\"model\":\"BMW\"}"));

        adapter.read(filter, new Callback<List<Data>>() {
            @Override
            public void onSuccess(List<Data> data) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                latch.countDown();
                Logger.getLogger(getClass().getSimpleName()).log(Level.SEVERE, TAG, e);
            }
        });
        latch.await(50000, TimeUnit.MILLISECONDS);

        verify(factory).get(new URL(urlNoSlash.toString() + "?limit=10&model=BMW&token=token"), 60000);
    }

    /**
     * This test tests the default paging configuration.
     */
    @Test()
    public void testLinkPagingReturnsData() throws InterruptedException, NoSuchFieldException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, Exception {
        Pipeline pipeline = new Pipeline(url);

        final HttpStubProvider provider = new HttpStubProvider(url, new HeaderAndBody(SERIALIZED_POINTS.getBytes(), new HashMap<String, Object>()));

        PageConfig pageConfig = new PageConfig();
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());

        PipeConfig pipeConfig = new PipeConfig(url, ListClassId.class);
        pipeConfig.setRequestBuilder(new GsonRequestBuilder(builder.create()));
        pipeConfig.setPageConfig(pageConfig);

        Pipe<ListClassId> dataPipe = pipeline.pipe(ListClassId.class, pipeConfig);
        Object restRunner = UnitTestUtils.getPrivateField(dataPipe, "restRunner");

        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                return provider;
            }
        });

        ReadFilter onePageFilter = new ReadFilter();

        onePageFilter.setLimit(1);
        runRead(dataPipe, onePageFilter);
        List<ListClassId> result = runRead(dataPipe, onePageFilter);

        assertNotNull(result);
        assertFalse(result instanceof PagedList);

    }

    /**
     * This test tests the default paging configuration.
     */
    @Test
    public void testDefaultPaging() throws InterruptedException, NoSuchFieldException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException,
            URISyntaxException {
        Pipeline pipeline = new Pipeline(url);

        PageConfig pageConfig = new PageConfig();
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(Point.class, new RestAdapterTest.PointTypeAdapter());

        PipeConfig pipeConfig = new PipeConfig(url, ListClassId.class);
        pipeConfig.setRequestBuilder(new GsonRequestBuilder(builder.create()));
        pipeConfig.setPageConfig(pageConfig);

        Pipe<ListClassId> dataPipe = pipeline.pipe(ListClassId.class, pipeConfig);
        Object restRunner = UnitTestUtils.getPrivateField(dataPipe, "restRunner");

        UnitTestUtils.setPrivateField(restRunner, "httpProviderFactory", new Provider<HttpProvider>() {
            @Override
            public HttpProvider get(Object... in) {
                HashMap<String, Object> headers = new HashMap<String, Object>(1);
                headers
                        .put(
                                "Link",
                                "<http://example.com/TheBook/chapter2>; rel=\"previous\";title=\"previous chapter\",<http://example.com/TheBook/chapter3>; rel=\"next\";title=\"next chapter\"");
                HttpStubProvider provider = new HttpStubProvider(url, new HeaderAndBody(SERIALIZED_POINTS.getBytes(), headers));

                return provider;
            }
        });

        ReadFilter onePageFilter = new ReadFilter();
        onePageFilter.setLimit(1);
        List<ListClassId> resultList = runRead(dataPipe, onePageFilter);
        assertTrue(resultList instanceof PagedList);
        WrappingPagedList<ListClassId> pagedList = (WrappingPagedList<ListClassId>) resultList;

        assertEquals(new URI("http://example.com/TheBook/chapter3"), pagedList.getNextFilter().getLinkUri());
        assertEquals(new URI("http://example.com/TheBook/chapter2"), pagedList.getPreviousFilter().getLinkUri());
    }

    @Test
    public void testBuildPagedResultsFromHeaders() throws Exception {
        PageConfig pageConfig = new PageConfig();
        pageConfig.setMetadataLocation(PageConfig.MetadataLocations.HEADERS);

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setPageConfig(pageConfig);

        RestAdapter adapter = new RestAdapter(Data.class, url, config);
        List<Data> list = new ArrayList<Data>();
        HeaderAndBody response = new HeaderAndBody(new byte[] {}, new HashMap<String, Object>() {
            {
                put("next", "chapter3");
                put("previous", "chapter2");
            }
        });
        JSONObject where = new JSONObject();
        Object restRunner = UnitTestUtils.getPrivateField(adapter, "restRunner");
        Method method = restRunner.getClass().getDeclaredMethod("computePagedList", List.class, HeaderAndBody.class, JSONObject.class, Pipe.class);
        method.setAccessible(true);

        WrappingPagedList<Data> pagedList = (WrappingPagedList<Data>) method.invoke(restRunner, list, response, where, adapter);
        assertEquals(new URI("http://server.com/context/chapter3"), pagedList.getNextFilter().getLinkUri());
        assertEquals(new URI("http://server.com/context/chapter2"), pagedList.getPreviousFilter().getLinkUri());

    }

    @Test
    public void testBuildPagedResultsFromBody() throws Exception {
        PageConfig pageConfig = new PageConfig();
        pageConfig.setMetadataLocation(PageConfig.MetadataLocations.BODY);
        pageConfig.setNextIdentifier("pages.next");
        pageConfig.setPreviousIdentifier("pages.previous");

        PipeConfig config = new PipeConfig(url, ListClassId.class);
        config.setPageConfig(pageConfig);

        RestAdapter adapter = new RestAdapter(Data.class, url, config);
        Object restRunner = UnitTestUtils.getPrivateField(adapter, "restRunner");

        List<Data> list = new ArrayList<Data>();
        HeaderAndBody response = new HeaderAndBody("{\"pages\":{\"next\":\"chapter3\",\"previous\":\"chapter2\"}}".getBytes(), new HashMap<String, Object>());
        JSONObject where = new JSONObject();
        Method method = restRunner.getClass().getDeclaredMethod("computePagedList", List.class, HeaderAndBody.class, JSONObject.class, Pipe.class);
        method.setAccessible(true);

        WrappingPagedList<Data> pagedList = (WrappingPagedList<Data>) method.invoke(restRunner, list, response, where, adapter);
        assertEquals(new URI("http://server.com/context/chapter3"), pagedList.getNextFilter().getLinkUri());
        assertEquals(new URI("http://server.com/context/chapter2"), pagedList.getPreviousFilter().getLinkUri());

    }

    private <T> List<T> runRead(Pipe<T> restPipe) throws InterruptedException {
        return runRead(restPipe, null);
    }

    /**
     * Runs a read method, returns the result of the call back and makes sure no
     * exceptions are thrown
     *
     * @param restPipe
     */
    private <T> List<T> runRead(Pipe<T> restPipe, ReadFilter readFilter) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean hasException = new AtomicBoolean(false);
        final AtomicReference<List<T>> resultRef = new AtomicReference<List<T>>();

        restPipe.read(readFilter, new Callback<List<T>>() {
            @Override
            public void onSuccess(List<T> data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                hasException.set(true);
                Logger.getLogger(RestAdapterTest.class.getSimpleName()).log(Level.SEVERE, e.getMessage(), e);
                latch.countDown();
            }
        });

        latch.await(2, TimeUnit.SECONDS);
        Assert.assertFalse(hasException.get());

        return resultRef.get();
    }

    public final static class ListClassId {

        List<Point> points = new ArrayList<Point>(10);
        @RecordId
        String id = "1";

        public ListClassId(boolean build) {
            if (build) {
                for (int i = 0; i < 10; i++) {
                    points.add(new Point(i, i * 2));
                }
            }
        }

        public ListClassId() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                return points.equals(((ListClassId) obj).points);
            } catch (Throwable ignore) {
                return false;
            }
        }
    }

    private static class PointTypeAdapter implements InstanceCreator, JsonSerializer, JsonDeserializer {

        @Override
        public Object createInstance(Type type) {
            return new Point();
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.addProperty("x", ((Point) src).x);
            object.addProperty("y", ((Point) src).y);
            return object;
        }

        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Point(json.getAsJsonObject().getAsJsonPrimitive("x").getAsInt(),
                    json.getAsJsonObject().getAsJsonPrimitive("y").getAsInt());
        }
    }

    public static final class Point implements Serializable {

        public int x, y;

        public Point() {
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 19 * hash + this.x;
            hash = 19 * hash + this.y;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Point other = (Point) obj;
            if (this.x != other.x) {
                return false;
            }
            if (this.y != other.y) {
                return false;
            }
            return true;
        }
    }
}
