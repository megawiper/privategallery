package com.privategallery.app.core.navigation

/**
 * Argument keys shared across destinations.
 * Only lightweight identifiers are ever passed — never full media/image objects.
 */
object NavArgs {
    const val MEDIA_ID = "mediaId"
    const val ALBUM_ID = "albumId"
    const val SEARCH_QUERY = "searchQuery"
    const val VIEWER_START_INDEX = "viewerStartIndex"
    const val SOURCE_COLLECTION = "sourceCollection"
    const val IS_PRIVATE = "isPrivate"
}

sealed class Destination(val route: String) {

    data object Splash : Destination("splash")
    data object Welcome : Destination("welcome")
    data object Permission : Destination("permission")

    data object Home : Destination("home")
    data object Albums : Destination("albums")
    data object AlbumDetails : Destination(
        "album_details/{${NavArgs.ALBUM_ID}}"
    ) {
        fun createRoute(albumId: String) = "album_details/$albumId"
    }

    data object Favorites : Destination("favorites")
    data object Search : Destination("search")

    data object ImageViewer : Destination(
        "image_viewer/{${NavArgs.SOURCE_COLLECTION}}/{${NavArgs.VIEWER_START_INDEX}}?${NavArgs.IS_PRIVATE}={${NavArgs.IS_PRIVATE}}"
    ) {
        fun createRoute(sourceCollection: String, startIndex: Int, isPrivate: Boolean = false) =
            "image_viewer/$sourceCollection/$startIndex?${NavArgs.IS_PRIVATE}=$isPrivate"
    }

    data object VideoViewer : Destination(
        "video_viewer/{${NavArgs.MEDIA_ID}}?${NavArgs.IS_PRIVATE}={${NavArgs.IS_PRIVATE}}"
    ) {
        fun createRoute(mediaId: String, isPrivate: Boolean = false) =
            "video_viewer/$mediaId?${NavArgs.IS_PRIVATE}=$isPrivate"
    }

    data object Editor : Destination(
        "editor/{${NavArgs.MEDIA_ID}}"
    ) {
        fun createRoute(mediaId: String) = "editor/$mediaId"
    }

    data object MediaInfo : Destination(
        "media_info/{${NavArgs.MEDIA_ID}}?${NavArgs.IS_PRIVATE}={${NavArgs.IS_PRIVATE}}"
    ) {
        fun createRoute(mediaId: String, isPrivate: Boolean = false) =
            "media_info/$mediaId?${NavArgs.IS_PRIVATE}=$isPrivate"
    }

    data object Trash : Destination("trash")
    data object Duplicates : Destination("duplicates")

    data object Settings : Destination("settings")
    data object SecuritySettings : Destination("security_settings")

    data object Lock : Destination(
        "lock?${NavArgs.IS_PRIVATE}={${NavArgs.IS_PRIVATE}}"
    ) {
        fun createRoute(isPrivate: Boolean) = "lock?${NavArgs.IS_PRIVATE}=$isPrivate"
    }

    data object PrivateGalleryHome : Destination("private_home")
    data object PrivateAlbumDetails : Destination(
        "private_album_details/{${NavArgs.ALBUM_ID}}"
    ) {
        fun createRoute(albumId: String) = "private_album_details/$albumId"
    }
    data object PrivateSearch : Destination("private_search")
}
