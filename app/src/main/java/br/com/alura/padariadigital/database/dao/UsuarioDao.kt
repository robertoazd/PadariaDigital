package br.com.alura.padariadigital.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.alura.padariadigital.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun salva(usuario: Usuario)

    @Query("""SELECT * FROM Usuario 
        WHERE id = :usuarioId 
        AND senha = :senha
        """)
    suspend fun autentica(
        usuarioId: String,
        senha: String
    ): Usuario?

    @Query("SELECT * FROM Usuario Where id = :usuarioId")
    fun buscaPorId(usuarioId: String): Flow<Usuario>

    @Query("SELECT * FROM Usuario")
    fun buscaTodos(): Flow<List<Usuario>>
}