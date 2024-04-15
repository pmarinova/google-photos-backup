package pm.google.photos.backup.auth;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;

import pm.google.photos.backup.auth.ClientSecretJson.ClientInfo;


public class GoogleAuthFlow {

	private final File clientSecretJsonFile;

	private final File dataStoreDirectory;

	private ClientInfo oauthClientInfo;

	private AuthorizationCodeFlow googleAuthFlow;

	private String redirectUri;

	public GoogleAuthFlow(File clientSecretJsonFile, File dataStoreDirectory) {
		this.clientSecretJsonFile = clientSecretJsonFile;
		this.dataStoreDirectory = dataStoreDirectory;
	}

	public Credentials authorize(String userId, List<String> scopes) {

		this.oauthClientInfo = loadClientSecretJson(this.clientSecretJsonFile);
		this.googleAuthFlow = createAuthFlow(this.oauthClientInfo, this.dataStoreDirectory, scopes);

		Credential credential = loadCredential(userId);

		if (credential == null) {
			credential = createAndStoreCredential(userId);
		} else {
			refreshCredential(credential);
		}

		AccessToken accessToken = new AccessToken(credential.getAccessToken(), new Date(credential.getExpirationTimeMilliseconds()));
		OAuth2Credentials oauthCredentials = OAuth2Credentials.create(accessToken);

		return oauthCredentials;
	}

	private ClientInfo loadClientSecretJson(File clientSecretJsonFile) {
		try {
			ClientSecretJson clientSecretJson = ClientSecretJson.load(clientSecretJsonFile);
			return clientSecretJson.getClientInfo();
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read client secret from file " + clientSecretJsonFile.getAbsolutePath(), ex);
		}
	}

	private AuthorizationCodeFlow createAuthFlow(
			ClientInfo clientInfo,
			File dataStoreDirectory,
			List<String> scopes) {

		try {
			return new AuthorizationCodeFlow.Builder(
						BearerToken.authorizationHeaderAccessMethod(),
						new NetHttpTransport(),
						new GsonFactory(),
						new GenericUrl(clientInfo.getTokenUri()),
						new ClientParametersAuthentication(clientInfo.getClientId(), clientInfo.getClientSecret()),
						clientInfo.getClientId(),
						clientInfo.getAuthUri())
					.setDataStoreFactory(new FileDataStoreFactory(dataStoreDirectory))
					.setScopes(scopes)
					.build();

		} catch (IOException ex) {
			throw new RuntimeException("Failed to set data store factory", ex);
		}
	}

	private Credential loadCredential(String userId) {
		try {
			return this.googleAuthFlow.loadCredential(userId);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to load credentials", ex);
		}
	}

	private Credential createAndStoreCredential(String userId) {
		try {
			String authCode = getAuthorizationCode();
			AuthorizationCodeTokenRequest authTokenRequest = this.googleAuthFlow.newTokenRequest(authCode);
			authTokenRequest.setRedirectUri(this.redirectUri); // this must be set to the same redirect URI used for the authorization code request
			return this.googleAuthFlow.createAndStoreCredential(authTokenRequest.execute(), userId);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to store credentials", ex);
		}
	}

	private void refreshCredential(Credential credential) {
		try {
			credential.refreshToken();
		} catch (IOException ex) {
			throw new RuntimeException("Failed to refresh access token", ex);
		}
	}

	private String getAuthorizationCode() {
		try {
			GoogleAuthCodeListener authCodeListener = new GoogleAuthCodeListener();
			InetSocketAddress authCodeListenerAddress = authCodeListener.getAddress();

			this.redirectUri = String.format("http://%s:%d", authCodeListenerAddress.getHostString(), authCodeListenerAddress.getPort());
			AuthorizationCodeRequestUrl authCodeRequestUrl = this.googleAuthFlow.newAuthorizationUrl();
			authCodeRequestUrl.setRedirectUri(this.redirectUri);

			Desktop.getDesktop().browse(authCodeRequestUrl.toURI());

			return authCodeListener.getCode();

		} catch (IOException ex) {
			throw new RuntimeException("Failed to get authorization code", ex);
		}
	}
}
