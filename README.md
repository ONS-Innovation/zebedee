# Zebedee

**NOTE: We are currently in the process of deprecating this service due to:**
 - _Performance limitations_
 - _Maintainability issues_
 - _Inability to scale effectively_
 - _Limitations of the underlying HTTP framework_ 
***

Zebedee is the CMS used by the ONS website and the internal website publishing system. It is a JSON API and does not 
have a user interface. It comes in 2 flavours:

## zebedee-reader
Zebedee-reader is read-only. It's used by [Babbage][1] (the public facing web frontend of the ONS website) it returns 
the published site content as JSON.

## zebedee-cms
Zebedee-cms is an extension of zebedee-reader. It's used by [Florence][2] and provides API endpoints for managing 
content, users, teams and publishing collecions. Zebedee-CMS is not public facing and requires authentication for the 
majority functionality. Pre-release content is encrypted and requries the appropriate permissions to be able to 
access it.
 
## Prerequisites 
- git
- Java 8
- Maven
- Docker

As mentioned Zebedee is a JSON API,  and does not have a user interface. The quickest and easiest way to use it is to 
set up a local copy of the "publishing" stack. Clone and set up the following projects following the README instructions 
in each repo:
- [Florence][2]
- [Babbage][1]
- [Sixteens][5]
- [dp-compose][6] 

### Getting started
***
_If you encounter any issues or notice anything missing from this guide please update/add any missing/helpful
 information and open a PR._

_Much appreciated._
_The Dev team_
***
_NOTE_: The following set guide will set up Zebedee in **"CMS"** mode as this is typically how the devlopers will run 
the stack locally. 

Getting the code

```
git clone git@github.com:ONSdigital/zebedee.git
```

### Database... 
Zebedee isn't backed by a database instead it uses a file system to store json files on disk ***. As a result it 
requires a specific directory structure in order to function correctly.

***
*** _We know this is a terrible idea - but in our defence this is a legacy hangover and we are actively working 
towards deprecating it._
***

To save yourself some pain you can use the [dp-zebedee-content][3] tool to create the required directory 
structure and populate the CMS with some "default content" - follow the steps in the [README][3] before going any further.
 
Once the script has run successfully copy the generated script from `dp-zebedee-utils/content/generated/run-cms.sh` into
the root dir of your Zebedee project. This bash script compiles and runs Zebedee setting typical dev default 
configuration and uses the content / directory structure generated by dp-zebedee-utils.

You may be required to make the bash script an executable before you can run it. If so run:

````bash
sudo chmod +x run-cms.sh
<Enter your password when prompted>
````  

### Running the publishing stack
In order to use Zebedee you will need to have the following other project running:
- Florence
- Babbage
- Sixteens
- dp-compose

Follow the steps in the README of each project.

#### Zebedee content 
Zebedee needs contents before it can be run. Basic demo content and file structures can be generated via the 
[dp-zebedee-content][3] by following the instructions in the 
[dp-zebedee-content readme](https://github.com/ONSdigital/dp-zebedee-content/blob/master/README.md)

#### Running zebedee 
```bash
./run-cms.sh
```

Assuming Zebedee has started without error head to [Florence login][4] and login with the default account:
```
email: florence@magicroundabout.ons.gov.uk
password: Doug4l
```
- If it's the first time logging in you will be prompted to change the password for that user.
- On the home screen create a new collection.
- Click `Create/edit` on the Collection Details screen.

If everything is working correctly you should now see the the ONS website displayed in the right hand pane. 
_Congratulations_ :tada:! Advanced to GO collect £200 :dollar:

Otherwise :violin: kindly ask someone from the dev team to help troubleshoot.

### Optional configuration options

| Environment variable          | Default   | Description
| ----------------------------- | --------- | -----------
| ENABLE_KAFKA                  | false     | Feature flag to send kafka messages when a collection is published
| KAFKA_ADDR                    | localhost:9092 | Comma seperated list of kafka brokers
| KAFKA_CONTENT_PUBLISHED_TOPIC | content-published | Kafka topic to send content-published messages to
| KAFKA_SEC_PROTO             | _unset_                              | if set to "TLS", kafka connections will use TLS
| KAFKA_SEC_CLIENT_KEY        | _unset_                              | if using TLS (see above), this is the path to the keystore (optional, used for client auth) - only used if `KAFKA_SEC_CLIENT_KEY_P12` is unset
| KAFKA_SEC_CLIENT_KEY_P12    | _unset_                              | if using TLS (see above), this is a base64-encoded PKCS12 keystore (optional, used for client auth)

### New Central Keyring configuration
The new central keyring feature is currently behind a feature flag:
```bash
export ENABLE_CENTRALISED_KEYRING=true/false
```
- If enabled Zebedee will attempt to read/write from the new central keyring and default to the legacy keyring if 
unsuccessful. 
- If disabled Zebedee will add/remove keys from both legacy and central keyring implementations but will 
only read from the legacy keyring.

The central keyring requires encryption config to be provided in app configutation. These secrets can be generated 
using the [collection-keyring-secrets-generator][7].

***
### :spider: :spider: :spider: :spider:
### Legacy dataset versions defect
There is currently an intermittant defect where the previous versions of a dataset are not being correctly added to
 the reviewed directory of the collection. This causes complications if it goes unnoticed and the collection is
  published. Work is ongoing to identify the cause.
  
To combat this an additonal check has been added to the `/approve` endpoint. If the collection contains dataset pages
 that are missing any of the expected versions the approval will be rejected.

 
### Bypassing the check

This check (if enabled) can be manually bypassed by a publisher user using an overrideKey on the approval request. 

**This should only be used as a last resort. Publising a collection in this state will require a manual datafix on the
 live environment and should only be done with service manager approval.**
 
To bypass this check:
- Login into Florence and use the Chrome developer tools to get the collection ID and auth token for your user
 (remember you must be a publisher user). 
- Use the `dp-cli` tool to access to publishing / publishing_mount
- Run `sudo docker ps -a` to get the `IP`and `port` for the publishing Zebedee instance
- Generate an override key - The number of minutes remaining until midnight **(UTC)**. You can use the `dp` tool to
 calculate this for you - `dp override-key`
- From the publishing box run the following `curl` command:
 
 ```bash
 curl -H "X-Florence-Token: <ZEBEDEE_SESSION_TOKEN>" -XPOST "http://<DOMAIN>/approve/<COLLECTION_ID>?overrideKey
=<OVERRIDE_KEY>" | jq .
 ```
 
 - `ZEBEDEE_SESSION_TOKEN` - A valid Zebedee session token for your user.
 - `<DOMAIN>` - the address of the Zebedee publishing instance.
 - `<COLLECTION_ID>` - the ID of the collection to be approved.
 - `<OVERRIDE_KEY>` - the secret key required to override the check.
 
If successful you should get something similar too:

```bash
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100     4  100     4    0     0    285      0 --:--:-- --:--:-- --:--:--   285
true
```
If this is fails try again after regenerating an override key as it may have expired before your request was sent.  
***

#### Service authentication with Zebedee

1) Login to florence using: `curl -X POST -d '{"email":"florence@magicroundabout.ons.gov.uk","password":"<your password>"}' http://localhost:8082/login`
2) Make a note of the `access_token` that gets returned in the headers
3) Create an admin service key: `curl -X POST http://localhost:8082/service -H "X-Florence-Token: <access_token>" -d '{"id":"admin"}'`
4) Make a note of the service token that gets returned in the response body
5) Set the environment variable:
`export SERVICE_AUTH_TOKEN=<YOUR_SERVICE_TOKEN>` replacing the token with that one you got in step 4
6) Restart zebedee and authenticating services


[1]: https://github.com/ONSdigital/babbage
[2]: https://github.com/ONSdigital/florence
[3]: https://github.com/ONSdigital/dp-zebedee-content
[4]: http://localhost:8081/florence/login
[5]: https://github.com/ONSdigital/sixteens
[6]: https://github.com/ONSdigital/dp-compose
[7]: collection-keyring-secrets-generator/README.md

#### SERVICE_AUTH_TOKEN creation for Production

Please see document in dp-operations:

[dp-operations/guides SERVICE_AUTH_TOKEN-for-Production](https://github.com/ONSdigital/dp-operations/blob/main/guides/SERVICE_AUTH_TOKEN-for-Production.md)
