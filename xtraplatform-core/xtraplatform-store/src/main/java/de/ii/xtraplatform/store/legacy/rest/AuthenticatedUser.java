/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;

import java.util.ArrayList;
import java.util.List;

/** @author fischer */
public class AuthenticatedUser {

  private String id;
  private Role role;
  private String orgId;
  private List<String> groups;

  public AuthenticatedUser() {
    this.role = Role.NONE;
    this.groups = new ArrayList<>();
  }

  public AuthenticatedUser(String id) {
    this();
    this.id = id;
  }

  public AuthenticatedUser(String orgId, String id) {
    this(id);
    this.orgId = orgId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }
}
