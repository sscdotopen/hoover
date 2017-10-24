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

import com.restfb._
import com.restfb.exception.{FacebookGraphException, FacebookOAuthException}
import com.restfb.json.JsonObject
import com.restfb.types.{Comment => FacebookComment, Post => FacebookPost}
import org.joda.time.{DateTime, Days}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class Synchronization(appId: String, appSecret: String, onlySyncPostsOlderThan: Int = 31) {

  private[this] val logger = LoggerFactory.getLogger(classOf[Synchronization])
  private[this] val token = new DefaultFacebookClient().obtainAppAccessToken(appId, appSecret)
  private[this] val client = new DefaultFacebookClient(token.getAccessToken, new ETagWebRequestor(),
    new DefaultJsonMapper(), Version.VERSION_2_5)

  private[this] val postsLimit = Parameter.`with`("limit", 100.toString)
  private[this] val pagingLimit = Parameter.`with`("limit", 500.toString)

  def sync(storage: Storage, page: Page) {

    logApiRequest(s"/${page.id}/feed")

    val connection = client.fetchConnection(s"/${page.id}/feed", classOf[FacebookPost], postsLimit)

    SyncUtils.paged(connection) {
      fbPost =>

        val postId = fbPost.getId

        if (storage.postExists(postId)) {
          logger.info(s"(${page.name}) Skipping post ${fbPost.getId} (${fbPost.getCreatedTime}), already synchronized.")
        } else {
          if (ageInDays(fbPost) > onlySyncPostsOlderThan) {

            val comments = SyncUtils.fetchRobust { _ => fetchCommentsFor(page.name, postId) }
            val likes = SyncUtils.fetchRobust { _ => fetchLikesFor(page.name, postId) }

            val post =
              Post(page.name, postId, fbPost.getMessage, fbPost.getCreatedTime, likes, comments)

            storage.addPost(post)

            logger.info(s"(${page.name}) STORING POST ${fbPost.getId} (${fbPost.getCreatedTime}).")
          } else {
            logger.info(s"(${page.name}) Skipping post ${fbPost.getId} (${fbPost.getCreatedTime}), too young.")
          }
        }
    }

  }

  def ageInDays(post: FacebookPost) = {
    val now = new DateTime().toLocalDate
    val postCreationDate = new DateTime(post.getCreatedTime).toLocalDate

    Days.daysBetween(postCreationDate, now).getDays
  }

  def logApiRequest(uri: String, depth: Int = 0) = {
    val indent = "  " * depth
    logger.info(s"API REQUEST - ${indent}${uri}")
  }

  def fetchLikesFor(pageName: String, id: String, depth: Int = 0): Seq[User] = {

    logApiRequest(s"(${pageName}) ${id}/likes", depth)

    val connection = client.fetchConnection(s"${id}/likes", classOf[JsonObject], pagingLimit)

    SyncUtils.paged(connection) { user =>
      val id = user.getString("id")
      val name = if (user.has("name")) {
        user.getString("name")
      } else {
        ""
      }
      User(id, name)
    }
  }

  def fetchCommentsFor(pageName: String, id: String, depth: Int = 0): Seq[Comment] = {

    logApiRequest(s"(${pageName}) ${id}/comments", depth)

    val connection = client.fetchConnection(s"${id}/comments", classOf[FacebookComment], pagingLimit)

    SyncUtils.paged(connection) { facebookComment =>

      val likes = SyncUtils.fetchRobust { _ => fetchLikesFor(pageName, facebookComment.getId) }

      Comment(facebookComment.getId, facebookComment.getMessage, facebookComment.getCreatedTime,
        User(facebookComment.getFrom.getId, facebookComment.getFrom.getName),
        likes, fetchCommentsFor(pageName, facebookComment.getId, depth + 1))
    }
  }

}

object SyncUtils {

  val logger = LoggerFactory.getLogger(getClass)

  /* somehow a flatmap over the iterator does not work... */
  def paged[A, B](connection: Connection[A])(transform: A => B): Seq[B] = {

    var results = new ListBuffer[B]()
    val iterator = connection.iterator()

    while (iterator.hasNext) {
      val nextElements = iterator.next()

      results ++= nextElements.map { transform }
    }

    results
  }

  def fetchRobust[T](func: Unit => Seq[T]) = {
    try {
      func()
    } catch {

      case e: FacebookGraphException =>
        // Ignore "unknown errors..."
        if (e.getErrorCode == 100) {
          Seq.empty[T]
        } else {
          throw e
        }

      case e: FacebookOAuthException =>
        // Ignore "unknown errors..."
        if (e.getErrorCode == 1 && (e.getErrorSubcode == null || e.getErrorSubcode == 99)) {
          Seq.empty[T]
        } else {
          throw e
        }
    }
  }
}