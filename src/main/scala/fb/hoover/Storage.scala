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

import java.io.File

import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import org.mapdb.{DBMaker, Serializer}
import org.json4s._
import org.json4s.native.Serialization
import java.nio.charset.StandardCharsets

import scala.collection.JavaConversions._

object Storage {
  def fetchPosts(folder: String): Seq[Post] = {
    val storage = new Storage(folder)
    val posts = storage.allPosts()
    storage.close()
    posts
  }
}

class Storage(folder: String) {

  implicit object DateTimeOrdering extends Ordering[DateTime] {
    def compare(d1: DateTime, d2: DateTime) = d1.compareTo(d2)
  }

  private[this] val database = DBMaker.fileDB(new File(folder, "posts.db")).make()
  private[this] val posts = database.hashMap("posts", Serializer.STRING, Serializer.STRING)
  implicit val formats = Serialization.formats(NoTypeHints)

  def asJson() = {
    posts.values().toSeq
  }

  def allPosts() = {
    posts
      .values()
      .map { Serialization.read[Post](_) }
      .toSeq
      .sortBy { _.created }
      .reverse
  }

  def postIds(): Set[String] = {
    posts.keySet().toSet
  }

  def getPost(id: String): Post = {
    Serialization.read[Post](posts.get(id))
  }

  def postExists(id: String): Boolean = {
    posts.containsKey(id)
  }

  def addPost(post: Post): Unit = {
    val asJson = Serialization.write(post)
    posts.put(post.id, asJson)
    database.commit()

    FileUtils.write(new File(folder, "posts.json"), asJson, StandardCharsets.UTF_8, true)
  }


  def close() = {
    database.commit()
    database.close()
  }

}
