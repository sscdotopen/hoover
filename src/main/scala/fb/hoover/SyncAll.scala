/**
  *  Synchronize public fb page data, Copyright (C) 2017 sscdotopen
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
  *
  */
package fb.hoover

object SyncAll extends App {

  if (args.length < 8 || args.length % 2 != 0) {
    System.err.println(
      """
        |Required arguments:
        |
        |--output <directory> --appId <fb-app-id> --appSecret <fb-app-secret> --delay <delay-in-days> \
        |--pages <list-of-id-name-pairs>
        |
        |Example:
        |
        |--output /home/me/data --appId 12345 --appSecret xKrot37a --delay 31 --pages 581482171869846,idt
        |
      """.stripMargin)
    System.exit(-1)
  }

  val namedArgs = args
    .grouped(2)
    .map { group => group.head -> group.last }
    .toMap

  val storage = new Storage(namedArgs("--output"))

  val pages = namedArgs("--pages").split(",")
    .grouped(2)
    .map { group => Page(group.head, group.last) }

  pages.foreach { page =>
    new Synchronization(namedArgs("--appId"), namedArgs("--appSecret"), namedArgs("--delay").toInt)
      .sync(storage, page)
  }

  storage.close()
}
