/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.confabricate;

/**
 * A configuration object that is only accessible while in-game.
 *
 * <p>On a dedicated server, this configuration is available any time after the
 * server constructor has happened.</p>
 *
 * <p>When used on the logical client, this configuration is available when the
 * player is ingame, and is reloaded after any registry or tag sync packets.</p>
 *
 * @param <V> value type
 */
public interface GameScoped<V> {

}
