/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.domain;

import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.json.commands.RedisJsonCommands;

public interface Redis extends Volatile2 {

  JedisCommands cmd();

  RedisJsonCommands json();

  RedisPubSub pubsub();
}
