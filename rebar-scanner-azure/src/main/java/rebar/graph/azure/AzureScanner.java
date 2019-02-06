/**
 * Copyright 2018-2019 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.graph.azure;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.base.Strings;
import com.google.common.io.Closer;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.management.Azure;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rebar.graph.core.RebarGraph;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.GraphSchema;
import rebar.util.EnvConfig;
import rebar.util.Json;
import rebar.util.RebarException;
import rebar.util.UrlBuilder;

public class AzureScanner extends Scanner {

	static OkHttpClient globalPool = new OkHttpClient().newBuilder().build();

	OkHttpClient client;

	Azure azure;

	public Azure getAzureClient() {
		try {
			if (azure == null) {
				
				// this will do for now
				// https://github.com/Azure/azure-libraries-for-java/blob/master/AUTH.md
				azure = Azure.authenticate(AzureCliCredentials.create()).withDefaultSubscription();
			
			}
		} catch (IOException e) {
			throw new RebarException(e);
		}
		return azure;
	}

	@Override
	protected void init(RebarGraph g, Map<String, String> config) throws IOException {

		EnvConfig cfg = g.getEnvConfig();

		client = globalPool.newBuilder().addInterceptor(new AuthInterceptor()).build();
	}

	class AuthInterceptor implements Interceptor {

		@Override
		public Response intercept(Chain chain) throws IOException {

			Request request = chain.request().newBuilder().build();

			return chain.proceed(request);
		}

	}

	Logger logger = LoggerFactory.getLogger(AzureScanner.class);

	public JsonNode getUrl(String url) {

		Closer closer = Closer.create();
		try {
			Request request = new Request.Builder().url(url).get().build();

			Response response = client.newCall(request).execute();

			if (response.code() == 404) {
				throw new ResourceNotFoundException("could not load {}" + url);
			}
			if (response.isSuccessful()) {
				Reader r = response.body().charStream();
				closer.register(r);
				closer.register(response.body());
				closer.register(response);
				JsonNode n = Json.objectMapper().readTree(r);

				return n;
			} else {
				int code = response.code();
				List<String> headers = response.headers("content-type");
				if (!headers.isEmpty()) {
					String val = headers.get(0);
					if (val.contains("json")) {

						JsonNode n = Json.objectMapper().readTree(response.body().bytes());

						if (n.path("error").path("errors").path(0).path("reason").asText()
								.equals("accessNotConfigured")) {
							return MissingNode.getInstance();
						}
					}

				}
				response.body().bytes(); // consume
			}

		} catch (IOException e) {
			throw new RebarException(e);
		} finally {
			try {
				closer.close();
			} catch (IOException e) {
				logger.warn("problem", e);
			}
		}
		return MissingNode.getInstance();

	}

	@Override
	public void doScan() {
		subscriptionScanner().scan();
	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String id) {
		throw new UnsupportedOperationException();
	}



	public boolean isRegionEnabled(String name) {
		return Strings.nullToEmpty(name).startsWith("us-");
	}

	public AzureRequest request() {
		return new AzureRequest();
	}

	public class AzureRequest extends UrlBuilder {

		@Override
		public AzureRequest url(String u) {

			return super.url(u);
		}

		@Override
		public AzureRequest path(String path) {

			return super.path(path);
		}

		@Override
		public AzureRequest pathParam(String key, String val) {
			// TODO Auto-generated method stub
			return super.pathParam(key, val);
		}

		@Override
		public AzureRequest queryParam(String key, String val) {

			return super.queryParam(key, val);
		}

		public JsonNode exec() {
			return getUrl(toString());
		}
	}
	
	public SubscriptionScanner subscriptionScanner() {
		return new SubscriptionScanner(this);
	}
}
