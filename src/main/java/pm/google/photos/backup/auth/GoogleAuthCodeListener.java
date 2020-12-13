package pm.google.photos.backup.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GoogleAuthCodeListener {

	private final ServerSocket serverSocket;
	private Socket clientSocket;

	public GoogleAuthCodeListener() throws IOException {
		this.serverSocket = new ServerSocket(0, 1, InetAddress.getByName("localhost"));
	}

	public InetSocketAddress getAddress() {
		return (InetSocketAddress)this.serverSocket.getLocalSocketAddress();
	}

	public String getCode() throws IOException {

		this.clientSocket = serverSocket.accept();

		try (PrintWriter out = new PrintWriter(this.clientSocket.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()))) {

			String startLine = in.readLine();

			Pattern startLinePattern = Pattern.compile("GET \\S+\\?(\\S+) HTTP/1.1");
			Matcher startLineMatcher = startLinePattern.matcher(startLine);

			if (!startLineMatcher.matches())
				throw new RuntimeException("Unexpected request: " + startLine);

			String queryString = startLineMatcher.group(1);
			Map<String, String> queryParams = parseQueryString(queryString);

			if (!queryParams.containsKey("code"))
				throw new RuntimeException("Unexpected query string: " + queryString);

			final String RESPONSE = "Access granted!";
			final int CONTENT_LENGTH = RESPONSE.getBytes(StandardCharsets.US_ASCII).length;

			out.print("HTTP/1.1 200 OK\r\n");
			out.printf("Content-Length: %d\r\n", CONTENT_LENGTH);
			out.print("\r\n");
			out.print(RESPONSE);
			out.flush();

			return queryParams.get("code");

		} finally {
			this.clientSocket.close();
			this.serverSocket.close();
		}
	}

	private Map<String, String> parseQueryString(String queryString) {
		Map<String,String> queryParams = new HashMap<>();
		for (String queryParam : queryString.split("&")) {
			String[] paramKeyAndValue = queryParam.split("=");
			queryParams.put(paramKeyAndValue[0], paramKeyAndValue[1]);
		}
		return queryParams;
	}
}
