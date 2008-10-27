/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.google.dataconnector.client;

import com.google.dataconnector.client.ClientRegistrationUtil;
import com.google.dataconnector.client.testing.FakeClientConfiguration;
import com.google.dataconnector.registration.v2.AuthRequest;
import com.google.dataconnector.registration.v2.AuthResponse;
import com.google.dataconnector.registration.v2.RegistrationResponse;
import com.google.dataconnector.util.AuthenticationException;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.RegistrationException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Tests for the {@link ClientRegistrationUtil} class.
 * 
 * @author rayc@google.com (Ray Colline)
 *
 */
public class ClientRegistrationUtilTest extends TestCase {

  private AuthRequest authRequest;
  private FakeClientConfiguration fakeClientConfig;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    fakeClientConfig = new FakeClientConfiguration();
    authRequest = new AuthRequest();
    authRequest.setDomain(fakeClientConfig.getFakeLocalConf().getDomain());
    authRequest.setUser(fakeClientConfig.getFakeLocalConf().getUser());
    authRequest.setPassword(fakeClientConfig.getFakeLocalConf().getPassword());
  }

  /**
   * Creates mock socket from the input streams provided for use with the registration util.
   * 
   * @param is pre populated input stream such as a {@link ByteArrayInputStream}
   * @param os pre populated output stram such as a {@link ByteArrayOutputStream}
   * @return configured mock socket.
   * @throws IOException to satisfy Socket class.
   */
  private Socket getFakeSocket(InputStream is, OutputStream os) throws IOException {
    Socket fakeSocket = EasyMock.createMock(Socket.class);
    EasyMock.expect(fakeSocket.getInputStream()).andReturn(is);
    EasyMock.expect(fakeSocket.getOutputStream()).andReturn(os);
    EasyMock.replay(fakeSocket);
    return fakeSocket;
  }
  
  public void testAuthorize() throws IOException, ConnectionException, JSONException {
    
    // Successful case.
    AuthResponse authResponse = new AuthResponse();
    authResponse.setStatus(AuthResponse.Status.OK);
    InputStream is = new ByteArrayInputStream(
        (authResponse.toJson().toString() + "\n").getBytes());
    OutputStream os = new ByteArrayOutputStream();
    AuthRequest returnedAuthRequest = ClientRegistrationUtil.authorize(getFakeSocket(is, os),
        fakeClientConfig.getFakeLocalConf());
    assertEquals(returnedAuthRequest.getPassword(), 
        fakeClientConfig.getFakeLocalConf().getPassword());
    assertEquals(returnedAuthRequest.getDomain(), fakeClientConfig.getFakeLocalConf().getDomain());
    assertEquals(returnedAuthRequest.getUser(), fakeClientConfig.getFakeLocalConf().getUser());
    
    // Access Denied Case
    authResponse = new AuthResponse();
    authResponse.setStatus(AuthResponse.Status.ACCESS_DENIED);
    is = new ByteArrayInputStream((authResponse.toJson().toString() + "\n").getBytes());
    os = new ByteArrayOutputStream();
    boolean threwException = false;
    try {
      returnedAuthRequest = ClientRegistrationUtil.authorize(getFakeSocket(is, os),
          fakeClientConfig.getFakeLocalConf());
    } catch (AuthenticationException e) {
      threwException = true;
    } 
    assertTrue(threwException);
    
    // Mangled Server Response.
    is = new ByteArrayInputStream("SO NOT A REAL JSON STRING\n".getBytes());
    os = new ByteArrayOutputStream();
    threwException = false;
    try {
      returnedAuthRequest = ClientRegistrationUtil.authorize(getFakeSocket(is, os),
          fakeClientConfig.getFakeLocalConf());
    } catch (AuthenticationException e) {
      threwException = true;
      assertTrue(e.getMessage().startsWith("Mangled"));
    } 
    assertTrue(threwException);
  }
  
  public void testRegister() throws IOException, ConnectionException, JSONException {

    // Successful case. Will throw RegistrationException if it fails.
    RegistrationResponse responseJson = new RegistrationResponse();
    responseJson.setStatus(RegistrationResponse.Status.OK);
    InputStream is = new ByteArrayInputStream((responseJson.toJson().toString() + "\n").getBytes());
    OutputStream os = new ByteArrayOutputStream();
    ClientRegistrationUtil.register(getFakeSocket(is, os), authRequest, 
        fakeClientConfig.getFakeResourceRules());

    // Negative Server Response case:
    responseJson = new RegistrationResponse();
    responseJson.setStatus(RegistrationResponse.Status.REGISTRATION_ERROR);
    responseJson.setErrorMsg("Some registration error");
    is = new ByteArrayInputStream((responseJson.toJson().toString() + "\n").getBytes());
    os = new ByteArrayOutputStream();
    boolean threwException = false;
    try {
      ClientRegistrationUtil.register(getFakeSocket(is, os), authRequest,
          fakeClientConfig.getFakeResourceRules());
    } catch (RegistrationException e) {
      threwException = true;
    } 
    assertTrue(threwException);

    // Mangled Server Response
    is = new ByteArrayInputStream("SO NOT A VALID JSON PACKET\n".getBytes());
    os = new ByteArrayOutputStream();
    threwException = false;
    try {
      ClientRegistrationUtil.register(getFakeSocket(is, os), authRequest, 
          fakeClientConfig.getFakeResourceRules());
    } catch (RegistrationException e) {
      threwException = true;
      assertTrue(e.getMessage().startsWith("Mangled"));
    } 
    assertTrue(threwException);

  }
}
