# Flow

### Expectations
- Checkout these apis
  - /telemetry - what does it do?
  - /recipe/handshake - what does it do?
  - /recipe/session/refresh
    - generation of RT and AT
    - saving it
  - /recipe/signinup/code
    - Generation of code
    - saving it
  - /recipe/signinup/code/consume
    - checking the code and returning the user
  - /recipe/session
    - what does it do?

- Models used - DB 1
  - tables and relations
  - queries

- use of processStates
  - using history (an array of process states)
- cronjob setups and usage
- Config inits - supertokens's singleton implementation 
  - using synchronized-lock and hashmap
  - attaching it to main
- how is the server starting and running? how is the setup
  - use of putMainThreadToSleep
  - use of TomCat

- use of storage layer  



- what is the use of supertokens-core? plugin-interface?
  - supertokens-root is just acting as a tray table for hosting supertokens-core (mainly) and plugin-interface.
  - plugin-interface?
  
## Pointers
- there are some implementation Classes like PasswordlessStorage 

## common

- for comments on the code, supertokens-core has to be checked out to branch - debug-supertokens-core-upgrade

Basics of Supertokens Core - BSC
C1: it starts from src/main/java/io/supertokens/Main.java which calls Webserver's start. it has

- Main() method contains a lot of inits, cronjobs and processState registers
  - Process state is there to just capture state of the server in test.  
- cronjobs are to reap expired passwords
  - understand cronjobs by looking at scheduleWithFixedDelay on concurrent's ScheduledExecutorService which hits after delay.


=======================================

GET /recipe/session - grs
- req body?
- res body?
- gets session info like userId, userData, RT etc from sessionHandle

POST /recipe/session - prs
- req body?
- res body?
- generates sessionHandle, AT, RT etc

================================

/recipe/session/refresh -> res is expected to have

- req body

```javascript
{
    refreshToken: string;
    antiCsrfToken ? : string;
    enableAntiCsrf ? : boolean;
}
```

- res body

```typescript
let accessToken = response.accessToken;
let refreshToken = response.refreshToken;
let idRefreshToken = response.idRefreshToken;
let anticsrftoken = response.antiCsrfToken;
```



CODE: *rsr* - every session refresh it returns a new RT and new AT but that is if previous RT was valid (i.e. either 
it is 
current RT in the db or the previous one) if RT is not any of them, we throw 401 and logs out (cookie deletion part 
is written in supertokens-node part). Also
subsequent 
calls of 
getSession uses the accesstoken and not refresh token.





=======================================================

/recipe/handshake - for jwt

===========================================================

***/recipe/session/verify*** - this is called by getSession in js layer. whenever we need sessionInfo, we use this. e.
g. an
api that returns the email of the user (probably logged out); another example is one api call that wants to fetch
something on userId but js layer is stateless so it fetches session from backend using access token.

- req body

```typescript
let requestBody: {
    accessToken: string;
    antiCsrfToken?: string;
    doAntiCsrfCheck: boolean;
    enableAntiCsrf?: boolean;
};
```

- res body

```typescript
if (response.accessToken !== undefined) {
    setFrontTokenInHeaders(
        res,
        response.session.userId,
        response.accessToken.expiry,
        response.session.userDataInJWT
    );
    // READCODE BUNI MW3: we attach accesstoken to response. 
    attachAccessTokenToCookie(config, res, response.accessToken.token, response.accessToken.expiry);
    accessToken = response.accessToken.token;
}
```

- CODE: *rsv* 

============================================

**/recipe/signinup/code** - get new code



- req body

```typescript
let response = await input.options.recipeImplementation.createCode(
    "email" in input
        ? {
            userContext: input.userContext,
            email: input.email,
            userInputCode:
                input.options.config.getCustomUserInputCode === undefined
                    ? undefined
                    : await input.options.config.getCustomUserInputCode(input.userContext),
        }
        : {
            userContext: input.userContext,
            phoneNumber: input.phoneNumber,
            userInputCode:
                input.options.config.getCustomUserInputCode === undefined
                    ? undefined
                    : await input.options.config.getCustomUserInputCode(input.userContext),
        }
)
```

- res body has things like codeLifetime, preAuthSessionId
- CODE: rsc - match how it uses request body attributes. notice this api is only implemented in passwordless.

=================================================
  
/recipe/signinup/code/consume - verify code and give access /recipe/user - getUser

- req body

```typescript
{
    preAuthSessionId: input.preAuthSessionId,
        deviceId:input.deviceId,
        userInputCode:input.userInputCode,
        userContext:input.userContext,
}
```

- res body

```typescript
let user = response.user;

const session = await Session.createNewSession(input.options.res, user.id, {}, {}, input.userContext);
return {
    status: "OK",
    createdNewUser: response.createdNewUser,
    user: response.user,
    session,
};
```

where createNewSession has body

```typescript
let response = await SessionFunctions.createNewSession(helpers, userId, accessTokenPayload, sessionData);
attachCreateOrRefreshSessionResponseToExpressRes(config, res, response);
return new Session(
    helpers,
    response.accessToken.token,
    response.session.handle,
    response.session.userId,
    response.session.userDataInJWT,
    res
);
```

and createNewSession finally calls the recipe/session api

```typescript
let requestBody: {
  userId: string;
  userDataInJWT: any;
  userDataInDatabase: any;
  enableAntiCsrf?: boolean;
} = {
  userId,
  userDataInJWT: accessTokenPayload,
  userDataInDatabase: sessionData,
};
 let response = await helpers.querier.sendPostRequest(new NormalisedURLPath("/recipe/session"), requestBody);
```

so basically first only user is created when we call consume api, it returns user_id and we use email(userContext) 
that we have to call /recipe/session which returns new session that we use.

- rscc1 - see the flow

======================================================
POST /recipe/session creates new session and GET /recipe/session gets session - RS1




======================================================

POST /session/verify

SV1 - this is for jwt verification. so getsession get is all it is for non-jwt case.

- refresh_session api - if access_token is expired, use refresh_token to generate new refresh_token and access_token. if
  not then, provide do nothing. if refresh_token is expired, clear cookies.

- /session/code - register email and mark it as unverified, generate and send an otp.
- /session/verify - use code and email to make sure that code matches and mark it as verified if success. return 401 or
  200 based on match and set or clear cookies.

1. cookie set process



=======================================

### Open queries

#### startup

- CREATE TABLE IF NOT EXISTS passwordless_users (user_id CHAR(36) NOT NULL,email VARCHAR(256) UNIQUE,phone_number VARCHAR(256) UNIQUE,time_joined BIGINT UNSIGNED NOT NULL,PRIMARY KEY (user_id));
- CREATE TABLE IF NOT EXISTS passwordless_devices (device_id_hash CHAR(44) NOT NULL,email VARCHAR(256),phone_number VARCHAR(256),link_code_salt CHAR(44) NOT NULL,failed_attempts INT UNSIGNED NOT NULL,PRIMARY KEY (device_id_hash));
- SELECT * FROM session_access_token_signing_keys
- INSERT INTO session_access_token_signing_keys(created_at_time, value) VALUES(?, ?)

- SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM passwordless_devices WHERE device_id_hash = ?
- INSERT INTO passwordless_devices(device_id_hash, email, phone_number, link_code_salt, failed_attempts) VALUES(?, ?, ?, ?, 0)
- INSERT INTO passwordless_codes(code_id, device_id_hash, link_code_hash, created_at) VALUES(?, ?, ?, ?)
- SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM passwordless_devices WHERE device_id_hash = ?
- DELETE FROM passwordless_codes WHERE device_id_hash IN (
- DELETE FROM passwordless_devices WHERE device_id_hash IN (
- SELECT user_id, email, phone_number, time_joined FROM passwordless_users WHERE email = ?
- INSERT INTO all_auth_recipe_users(user_id, recipe_id, time_joined) VALUES(?, ?, ?)]
- INSERT INTO passwordless_users(user_id, email, phone_number, time_joined) VALUES(?, ?, ?, ?)
- INSERT INTO session_info(session_handle, user_id, refresh_token_hash_2, session_data, expires_at, jwt_user_payload, created_at_time) VALUES(?, ?, ?, ?, ?, ?, ?)

#### consume
- SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM passwordless_devices WHERE device_id_hash = ?
- DELETE FROM passwordless_codes WHERE device_id_hash IN (?)
- SELECT user_id, email, phone_number, time_joined FROM passwordless_users WHERE email = ?
- INSERT INTO all_auth_recipe_users(user_id, recipe_id, time_joined) VALUES(?, ?, ?)
- INSERT INTO passwordless_users(user_id, email, phone_number, time_joined) VALUES(?, ?, ?, ?)
- INSERT INTO session_info(session_handle, user_id, refresh_token_hash_2, session_data, expires_at, jwt_user_payload, created_at_time) VALUES(?, ?, ?, ?, ?, ?, ?)





## Findings

### interesting finds
1. Use of resourceDistributor which ties all singleton classes together.
2. ProcessState to log states
3. there is use of cronjobs but for what?
4. there is use of some getDevice_Transaction, commitTransaction  etc. what is happening here?


## HOOK

- understand some nuances of the code. (coding pattern, third party libraries etc)
- imagine how I would write the same code. how I will end-up with same codebase. Find many ways of doing this. 
- Checkout more features and find them in code. Not to document or anything but just know how to navigate. Pick few 
  features randomly.
  
