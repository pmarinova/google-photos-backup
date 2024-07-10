package pm.google.photos.backup.auth;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;


public class GoogleAuthFlow {

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	private static final HttpTransport HTTP_TRANSPORT = newTrustedHttpTransport();

	private static HttpTransport newTrustedHttpTransport() {
		try {
			return GoogleNetHttpTransport.newTrustedTransport();
		} catch (IOException | GeneralSecurityException ex) {
			throw new RuntimeException("Failed to create trusted HTTP transport", ex);
		}
	}


	private final File clientSecretJsonFile;

	private final File dataStoreDirectory;


	public GoogleAuthFlow(File clientSecretJsonFile, File dataStoreDirectory) {
		this.clientSecretJsonFile = clientSecretJsonFile;
		this.dataStoreDirectory = dataStoreDirectory;
	}


	public Credentials authorize(String userId, List<String> scopes) {
		GoogleClientSecrets clientSecret = loadClientSecretJson(clientSecretJsonFile);
		DataStoreFactory credentialDataStoreFactory = newFileDataStoreFactory(dataStoreDirectory);

		GoogleAuthorizationCodeFlow authFlow = newGoogleAuthFlow(clientSecret, credentialDataStoreFactory, scopes);

		Credential credential = authorize(authFlow, userId);
		refreshCredential(credential);

		return toOAuth2Credentials(credential);
	}


	private static Credential authorize(GoogleAuthorizationCodeFlow authFlow, String userId) {
		try {
			AuthorizationCodeInstalledApp.Browser noBrowser = (url) -> System.out.println(url);
			return new AuthorizationCodeInstalledApp(authFlow, new LocalServerReceiver(), noBrowser).authorize(userId);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to authorize", ex);
		}
	}


	private static GoogleClientSecrets loadClientSecretJson(File clientSecretJsonFile) {
		try (FileReader clientSecretJson = new FileReader(clientSecretJsonFile)) {
			return GoogleClientSecrets.load(JSON_FACTORY, clientSecretJson);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read client secret from file "
											+ clientSecretJsonFile.getAbsolutePath(), ex);
		}
	}


	private static DataStoreFactory newFileDataStoreFactory(File dataStoreDirectory) {
		try {
			return new FileDataStoreFactory(dataStoreDirectory);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to create data store factory "
											+ dataStoreDirectory.getAbsolutePath(), ex);
		}
	}


	private static GoogleAuthorizationCodeFlow newGoogleAuthFlow(
			GoogleClientSecrets clientSecret,
			DataStoreFactory dataStoreFactory,
			List<String> scopes) {
		try {
			return new GoogleAuthorizationCodeFlow
					.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecret, scopes)
					.setDataStoreFactory(dataStoreFactory)
					.build();
		} catch (IOException ex) {
			throw new RuntimeException("Failed to set data store factory", ex);
		}
	}


	private void refreshCredential(Credential credential) {
		try {
			credential.refreshToken();
		} catch (IOException ex) {
			throw new RuntimeException("Failed to refresh access token", ex);
		}
	}


	private static OAuth2Credentials toOAuth2Credentials(Credential credential) {
		AccessToken accessToken = new AccessToken(credential.getAccessToken(), new Date(credential.getExpirationTimeMilliseconds()));
		return OAuth2Credentials.create(accessToken);
	}
}
