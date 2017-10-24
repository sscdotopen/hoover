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

import java.util.Date
import org.joda.time.DateTime

case class Page(id: String, name: String)

case class User(id: String, name: String)

case class Comment(id: String, message: String, created: Date, author: User, likedBy: Seq[User],
    comments: Seq[Comment]) {

  def createdDateTime() = { new DateTime(created) }
}

case class Post(page: String, id: String, message: String, created: Date, likedBy: Seq[User], comments: Seq[Comment]) {

  def createdDateTime() = { new DateTime(created) }

  def pageId() = { id.split("_").head }
}