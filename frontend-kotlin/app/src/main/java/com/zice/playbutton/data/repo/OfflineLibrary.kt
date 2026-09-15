package com.zice.playbutton.data.repo

import com.zice.playbutton.data.local.AudioDownloads
import com.zice.playbutton.data.local.AudioUsage
import com.zice.playbutton.data.local.db.DownloadedPlaylistDao
import com.zice.playbutton.data.local.db.DownloadedPlaylistEntity
import com.zice.playbutton.data.local.db.SongCacheDao
import com.zice.playbutton.domain.DownloadedPlaylist
import com.zice.playbutton.domain.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que ocupan las descargas y cuántas playlists son. */
data class DownloadsUsage(
    val bytes: Long = 0L,
    val songs: Int = 0,
    val playlists: Int = 0,
)

/**
 * Qué se puede escuchar sin servidor: el audio descargado y el registro de a
 * qué playlist pertenece.
 *
 * Una playlist entra en el registro solo cuando el usuario pide descargarla y
 * acaba entera. El detalle de la playlist únicamente la pone al día —y solo
 * con conexión: sin ella la lista de canciones puede venir de una caché
 * incompleta y no serviría para decidir si la playlist sigue entera—.
 *
 * Y se anota, no se calcula al vuelo, porque cuando hace falta —al abrir la
 * app sin cobertura— no hay forma de saber qué canciones tiene una playlist ni
 * cómo se llama: los listados viven en memoria y se van con el proceso.
 */
@Singleton
class OfflineLibrary @Inject constructor(
    private val dao: DownloadedPlaylistDao,
    private val songCacheDao: SongCacheDao,
    private val audioDownloads: AudioDownloads,
) {
    val playlists: Flow<List<DownloadedPlaylist>> = dao.observeAll()
        .map { entities -> entities.map(DownloadedPlaylistEntity::toDomain) }

    /** Lo que se sabe de una playlist descargada, sin servidor de por medio. */
    suspend fun find(playlistId: Int): DownloadedPlaylist? =
        dao.find(playlistId)?.toDomain()

    suspend fun usage(): DownloadsUsage = withContext(Dispatchers.IO) {
        val audio: AudioUsage = audioDownloads.usage()
        DownloadsUsage(
            bytes = audio.bytes,
            songs = audio.songs,
            playlists = dao.observeAll().first().size,
        )
    }

    /** Si el usuario ha pedido esta playlist para escucharla sin conexión. */
    suspend fun isDownloaded(playlistId: Int): Boolean = dao.find(playlistId) != null

    /**
     * Anota la playlist como descargada, porque el usuario lo ha pedido. Si no
     * ha acabado entera —se canceló, faltó sitio o falló la red— no entra en
     * la lista: sin todo el audio no se puede escuchar sin conexión.
     */
    suspend fun register(playlist: Playlist, totalSongs: Int, savedSongs: Int) {
        val complete = totalSongs > 0 && savedSongs >= totalSongs
        if (!complete) {
            dao.delete(playlist.id)
            return
        }
        dao.upsert(
            DownloadedPlaylistEntity(
                playlistId = playlist.id,
                name = playlist.name,
                ownerName = playlist.ownerName,
                isArtist = playlist.isArtist,
                songCount = totalSongs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Pone al día una playlist que ya estaba descargada: sale de la lista en
     * cuanto deja de estar entera, por ejemplo al añadirle canciones nuevas
     * que aún no se han descargado, y si sigue entera se refrescan el nombre y
     * el recuento.
     *
     * Nunca mete una playlist que no estuviera. Que estén todas sus canciones
     * en el dispositivo no quiere decir que el usuario la haya pedido: lo
     * normal es que las haya bajado con otras listas que las comparten, y
     * darla por descargada prometería algo que se rompe en cuanto se borre
     * cualquiera de aquellas.
     */
    suspend fun refresh(playlist: Playlist, totalSongs: Int, savedSongs: Int) {
        if (!isDownloaded(playlist.id)) return
        register(playlist, totalSongs, savedSongs)
    }

    /**
     * Borra la descarga de una playlist. Las canciones que también estén en
     * otra playlist descargada se quedan: una misma canción suele repetirse
     * entre listas, y borrar «Verano» no puede dejar «Terraza» a medias.
     */
    suspend fun removeDownload(playlistId: Int) = withContext(Dispatchers.IO) {
        val own = songCacheDao.songsFor(playlistId).map { it.songId }
        val others = dao.observeAll().first()
            .filterNot { it.playlistId == playlistId }
            .map { other -> songCacheDao.songsFor(other.playlistId).map { it.songId } }

        audioDownloads.remove(songsOnlyIn(own, others))
        dao.delete(playlistId)
    }

    /** Vaciar el almacenamiento entero, desde configuración. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        audioDownloads.clear()
        dao.clear()
    }

    suspend fun forget(playlistId: Int) = dao.delete(playlistId)
}

private fun DownloadedPlaylistEntity.toDomain() = DownloadedPlaylist(
    id = playlistId,
    name = name,
    ownerName = ownerName,
    isArtist = isArtist,
    songCount = songCount,
)

/**
 * Las canciones que solo tiene esta playlist. Lo que comparta con otra
 * playlist descargada no se toca: una misma canción suele estar en varias
 * listas, y borrar la descarga de una no puede dejar las demás a medias.
 */
internal fun songsOnlyIn(own: List<Int>, others: List<List<Int>>): List<Int> {
    val elsewhere = others.flatten().toHashSet()
    return own.filterNot { it in elsewhere }
}
