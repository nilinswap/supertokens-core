/*
 *    Copyright (c) 2022, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.test.emailpassword;

import io.supertokens.ProcessState;
import io.supertokens.emailpassword.EmailPassword;
import io.supertokens.emailpassword.exceptions.WrongCredentialsException;
import io.supertokens.pluginInterface.STORAGE_TYPE;
import io.supertokens.pluginInterface.emailpassword.UserInfo;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.test.TestingProcessManager;
import io.supertokens.test.Utils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.*;

public class UserMigrationTest {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void testBasicUserMigration() throws Exception {
        String[] args = { "../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        // with bcrypt
        {
            String email = "test@example.com";
            String plainTextPassword = "testPass123";
            String passwordHash = "$2a$10$GzEm3vKoAqnJCTWesRARCe/ovjt/07qjvcH9jbLUg44Fn77gMZkmm";

            // migrate user with passwordHash
            EmailPassword.ImportUserResponse importUserResponse = EmailPassword
                    .importUserWithPasswordHashOrUpdatePasswordHashIfUserExists(process.main, email, passwordHash);
            // check that the user was created
            assertFalse(importUserResponse.didUserAlreadyExist);
            // try and sign in with plainTextPassword
            UserInfo userInfo = EmailPassword.signIn(process.main, email, plainTextPassword);

            assertEquals(userInfo.id, importUserResponse.user.id);
            assertEquals(userInfo.passwordHash, passwordHash);
            assertEquals(userInfo.email, email);
        }

        // with argon2
        {
            String email = "test2@example.com";
            String plainTextPassword = "testPass123";
            String passwordHash = "$argon2id$v=19$m=16,t=2,p=1$VG1Oa1lMbzZLbzk5azQ2Qg$kjcNNtZ/b0t/8HgXUiQ76A";

            // migrate user with passwordHash
            EmailPassword.ImportUserResponse importUserResponse = EmailPassword
                    .importUserWithPasswordHashOrUpdatePasswordHashIfUserExists(process.main, email, passwordHash);
            // check that the user was created
            assertFalse(importUserResponse.didUserAlreadyExist);
            // try and sign in with plainTextPassword
            UserInfo userInfo = EmailPassword.signIn(process.main, email, plainTextPassword);

            assertEquals(userInfo.id, importUserResponse.user.id);
            assertEquals(userInfo.passwordHash, passwordHash);
            assertEquals(userInfo.email, email);
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testUpdatingAUsersPasswordHash() throws Exception {
        String[] args = { "../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        String email = "test@example.com";
        String originalPassword = "testPass123";

        UserInfo signUpUserInfo = EmailPassword.signUp(process.main, email, originalPassword);

        // update passwordHash with new passwordHash
        String newPassword = "newTestPass123";
        String newPasswordHash = "$2a$10$uV17z2rVB3W5Rp4MeJeB4OdRX/Z7oFMLpUbdzyX9bDrk6kvZiOT1G";

        EmailPassword.ImportUserResponse response = EmailPassword
                .importUserWithPasswordHashOrUpdatePasswordHashIfUserExists(process.main, email, newPasswordHash);
        // check that the user already exists
        assertTrue(response.didUserAlreadyExist);

        // try signing in with the old password and check that it does not work
        Exception error = null;
        try {
            EmailPassword.signIn(process.main, email, originalPassword);
        } catch (WrongCredentialsException e) {
            error = e;
        }
        assertNotNull(error);

        // sign in with the newPassword and check that it works
        UserInfo userInfo = EmailPassword.signIn(process.main, email, newPassword);
        assertEquals(userInfo.email, signUpUserInfo.email);
        assertEquals(userInfo.id, signUpUserInfo.id);
        assertEquals(userInfo.timeJoined, signUpUserInfo.timeJoined);
        assertEquals(userInfo.passwordHash, newPasswordHash);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }
}
