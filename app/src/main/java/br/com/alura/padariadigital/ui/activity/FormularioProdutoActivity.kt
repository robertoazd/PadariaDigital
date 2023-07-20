package br.com.alura.padariadigital.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import br.com.alura.padariadigital.R
import br.com.alura.padariadigital.database.AppDatabase
import br.com.alura.padariadigital.database.dao.ProdutoDao
import br.com.alura.padariadigital.databinding.ActivityFormularioProdutoBinding
import br.com.alura.padariadigital.extensions.tentaCarregarImagem
import br.com.alura.padariadigital.model.Produto
import br.com.alura.padariadigital.model.Usuario
import br.com.alura.padariadigital.ui.dialog.FormularioImagemDialog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class FormularioProdutoActivity : UsuarioBaseActivity() {

    private val binding by lazy {
        ActivityFormularioProdutoBinding.inflate(layoutInflater)
    }
    private var url: String? = null
    private var produtoId = 0L
    private val produtoDao: ProdutoDao by lazy {
        val db = AppDatabase.instancia(this)
        db.produtoDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = "Cadastrar produto"
        configuraBotaoSalvar()
        binding.activityFormularioProdutoImagem.setOnClickListener {
            FormularioImagemDialog(this)
                .mostra(url) { imagem ->
                    url = imagem
                    binding.activityFormularioProdutoImagem.tentaCarregarImagem(url)
                }
        }
        tentaCarregarProduto()

        /*lifecycleScope.launch {
            dataStore.data.collect { preferences ->
                preferences[usuarioLogadoPreferences]?.let { usuarioId ->
                    usuarioDao.buscaPorId(usuarioId).collect {
                        Log.i("FormularioProduto" , "onCreate: $it")
                    }
                }
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        tentaBuscarProduto()
    }

    private fun tentaCarregarProduto() {
        produtoId = intent.getLongExtra(CHAVE_PRODUTO_ID, 0L)
    }

    private fun tentaBuscarProduto() {
        lifecycleScope.launch {
            produtoDao.buscaPorId(produtoId).collect {
                it?.let { produtoEncontrado ->
                    title = "Alterar produto"
                    val campoUsuarioId =
                        binding.activityFormularioProdutoUsuarioId
                    campoUsuarioId.visibility =
                        if (produtoEncontrado.salvoSemUsuario()) {
                            configuraCampoUsuario()
                            View.VISIBLE
                        } else
                            View.GONE
                    preencheCampos(produtoEncontrado)
                }
            }
        }
    }

    private fun configuraCampoUsuario() {
        lifecycleScope.launch {
            usuarios()
                .map { usuarios -> usuarios.map { it.id } }
                .collect { usuarios ->
                    configuraAutoCompleteTextView(usuarios)
                }
        }
    }

    private fun configuraAutoCompleteTextView(
        usuarios: List<String>
    ) {
        val campoUsuarioId =
            binding.activityFormularioProdutoUsuarioId
        val adapter = ArrayAdapter(
            this@FormularioProdutoActivity,
            R.layout.support_simple_spinner_dropdown_item,
            usuarios
        )
        campoUsuarioId.setAdapter(adapter)
        campoUsuarioId.setOnFocusChangeListener { _, focado ->
            if (!focado) {
                usuarioExistenteValido(usuarios)
            }
        }
    }

    private fun usuarioExistenteValido(
        usuarios: List<String>
    ): Boolean {
        val campoUsuarioId = binding.activityFormularioProdutoUsuarioId
        val usuarioId = campoUsuarioId.text.toString()
        if (!usuarios.contains(usuarioId)) {
            campoUsuarioId.error = "usuário inexistente!"
            return false
        }
        return true
    }

    private fun preencheCampos(produto: Produto) {
        url = produto.imagem
        binding.activityFormularioProdutoImagem
            .tentaCarregarImagem(produto.imagem)
        binding.activityFormularioProdutoNome
            .setText(produto.nome)
        binding.activityFormularioProdutoDescricao
            .setText(produto.descricao)
        binding.activityFormularioProdutoValor
            .setText(produto.valor.toPlainString())
    }

    private fun configuraBotaoSalvar() {
        val botaoSalvar = binding.activityFormularioProdutoBotaoSalvar

        botaoSalvar.setOnClickListener {

            lifecycleScope.launch {
                tentaSalvarProduto()
            }
        }
    }

    private suspend fun tentaSalvarProduto() {
        usuario.value?.let { usuario ->
            try {
                val usuarioId = defineUsuarioId(usuario)
                val produto = criaProduto(usuarioId)
                produtoDao.salva(produto)
                finish()
            } catch (e: RuntimeException) {
                Log.e("FormularioProduto", "configuraBotaoSalvar: ", e)
            }
        }
    }

    private suspend fun defineUsuarioId(usuario: Usuario): String = produtoDao
        .buscaPorId(produtoId)
        .first()?.let { produtoEncontrado ->
            if (produtoEncontrado.usuarioId.isNullOrBlank()) {
                val usuarios = usuarios()
                    .map { usuariosEncontrados ->
                        usuariosEncontrados.map { it.id }
                    }.first()
                if (usuarioExistenteValido(usuarios)) {
                    val campoUsuarioId =
                        binding.activityFormularioProdutoUsuarioId
                    return campoUsuarioId.text.toString()
                } else {
                    throw RuntimeException("Tentou salvar produto com usuário inexistente")
                }
            }
            null
        } ?: usuario.id

    private fun criaProduto(usuarioId: String): Produto {
        val campoNome = binding.activityFormularioProdutoNome
        val nome = campoNome.text.toString()
        val campoDescricao = binding.activityFormularioProdutoDescricao
        val descricao = campoDescricao.text.toString()
        val campoValor = binding.activityFormularioProdutoValor
        val valorEmTexto = campoValor.text.toString()
        val valor = if (valorEmTexto.isBlank()) {
            BigDecimal.ZERO
        } else {
            BigDecimal(valorEmTexto)
        }

        return Produto(
            id = produtoId,
            nome = nome,
            descricao = descricao,
            valor = valor,
            imagem = url,
            usuarioId = usuarioId
        )
    }
}