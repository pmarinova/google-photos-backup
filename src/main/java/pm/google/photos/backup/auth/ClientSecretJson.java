package pm.google.photos.backup.auth;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;


public class ClientSecretJson {

	private static final Gson GSON = new Gson();

	public static class ClientInfo {

		@SerializedName("project_id")
		private String projectId;

		@SerializedName("client_id")
		private String clientId;

		@SerializedName("client_secret")
		private String clientSecret;

		@SerializedName("auth_provider_x509_cert_url")
		private String authProviderCertUrl;

		@SerializedName("auth_uri")
		private String authUri;

		@SerializedName("token_uri")
		private String tokenUri;

		@SerializedName("redirect_uris")
		List<String> redirectUris;

		public String getProjectId() {
			return projectId;
		}

		public String getClientId() {
			return clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public String getAuthProviderCertUrl() {
			return authProviderCertUrl;
		}

		public String getAuthUri() {
			return authUri;
		}

		public String getTokenUri() {
			return tokenUri;
		}

		public List<String> getRedirectUris() {
			return redirectUris;
		}

		@Override
		public String toString() {
			return GSON.toJson(this);
		}
	}

	@SerializedName("installed")
	private ClientInfo clientInfo;

	public ClientInfo getClientInfo() {
		return clientInfo;
	}

	@Override
	public String toString() {
		return GSON.toJson(this);
	}

	public static ClientSecretJson load(File file) throws IOException {
		try (FileReader reader = new FileReader(file)) {
			return GSON.fromJson(reader, ClientSecretJson.class);
		}
	}
}
