### Update 2025

After the latest changes to the Google Photos APIs, it is no longer possible to download your Google Photos library this way.
Photos and videos can only be downloaded via the new Google Photos Picker API, which requires initiating a session and
directing the user to the Google Photos app to browse their library and select the photos and videos they want to share
with the third-party app (i.e. in our case the Google Photos Backup tool). This means there is no way for a user to 
grant an app read permission to their whole library, which is required for a tool such as Google Photos Backup.

---

Google Photos Backup is a Java tool for downloading all of your Google Photos to the local file system.

The Google Photos API has a quota limit for requests per project per day. This is why you need to create your own project to use the Google Photos Backup tool:

1. Create a Google API project
   1. Open the [Google API Console](https://console.developers.google.com/)
   2. Create a new project with id `google-photos-backup`
   3. Select the project and open the `Dashboard`
2. Enable the Google Photos API
   1. Click on `Enable APIs and Services` (or the `Library` link on the right-hand side menu)
   2. Search for `Photos Library API` and enable it
3. Configure the OAuth consent screen
   1. Click on `Credentials` on the right-hand side menu
   2. Click `Configure Consent Screen`
   3. Select `External` user type and then `Create`
   4. Enter `My Photos Backup` for the application name and click `Save`
4. Setup the OAuth client credentials
   1. Click on `Credentials` again and then `Create Credentials > OAuth client ID`
   2. Select `Desktop app` for application type
   3. Enter `google-photos-backup` for the client name
   4. Click `Create` and then `OK`
5. Download the client_secret.json file
   1. Click on the download button for the google-photos-backup client
   2. Save the file as `client_secret.json` in the project root
