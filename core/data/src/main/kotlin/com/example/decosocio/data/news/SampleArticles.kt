package com.example.decosocio.data.news

import com.example.decosocio.domain.model.Article
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Placeholder articles written for the demo (not DECO PROteste content). The UI labels them
 * "Artigo de exemplo". Replace with the real feed by setting NEWS_FEED_URL.
 */
object SampleArticles {
    fun all(today: LocalDate): List<Article> {
        fun daysAgo(days: Int) = today.minus(DatePeriod(days = days))
        return listOf(
            Article(
                id = "sample-energia",
                title = "Antes do inverno, compare o seu tarifário de eletricidade",
                summary = "Três passos para perceber se está a pagar demais pela energia e como mudar de comercializador sem custos.",
                body = "Com a chegada do frio, o consumo de eletricidade sobe e pequenas diferenças no preço do kWh pesam mais na fatura.\n\n" +
                    "Comece por reunir as últimas faturas e anotar a potência contratada, o consumo anual e o ciclo horário. " +
                    "Depois, use um simulador independente para comparar ofertas com o mesmo perfil de consumo.\n\n" +
                    "Mudar de comercializador é gratuito e não implica cortes no fornecimento. Confirme apenas se o contrato atual tem período de fidelização.",
                category = "Energia",
                publishedOn = daysAgo(1),
                imageUrl = null,
                url = null,
                membersOnly = false,
                isSample = true,
            ),
            Article(
                id = "sample-livre-resolucao",
                title = "Compras online: 14 dias para desistir sem dar justificação",
                summary = "O direito de livre resolução protege as compras à distância. Saiba como exercê-lo e quando não se aplica.",
                body = "Nas compras feitas à distância, como online ou por telefone, tem normalmente 14 dias para desistir do contrato sem indicar o motivo.\n\n" +
                    "O prazo conta a partir da receção do bem ou, nos serviços, da celebração do contrato. O vendedor deve devolver o valor pago, incluindo os portes de envio normais, até 14 dias após ser informado.\n\n" +
                    "Há exceções, como bens personalizados ou conteúdos digitais já descarregados com o seu consentimento. Guarde sempre a prova do pedido.",
                category = "Direitos",
                publishedOn = daysAgo(3),
                imageUrl = null,
                url = null,
                membersOnly = false,
                isSample = true,
            ),
            Article(
                id = "sample-aspiradores",
                title = "Aspiradores sem fio: o que realmente conta na escolha",
                summary = "Autonomia, sucção em tapetes e facilidade de esvaziar o depósito fazem mais diferença do que a potência anunciada.",
                body = "A potência indicada na caixa diz pouco sobre a eficácia de um aspirador sem fio.\n\n" +
                    "Nos ensaios comparativos, os critérios que mais separam os modelos são a capacidade de recolher pó em tapetes, a autonomia real no modo normal e o ruído.\n\n" +
                    "Antes de comprar, confirme o preço e a disponibilidade de baterias de substituição: é a peça que mais cedo se desgasta.",
                category = "Testes",
                publishedOn = daysAgo(5),
                imageUrl = null,
                url = null,
                membersOnly = true,
                isSample = true,
            ),
            Article(
                id = "sample-burlas-sms",
                title = "SMS falsos de entregas: como reconhecer a burla",
                summary = "Mensagens a pedir o pagamento de portes ou taxas alfandegárias são um esquema frequente. Veja os sinais de alerta.",
                body = "Se recebeu uma mensagem a dizer que uma encomenda está retida e que precisa de pagar uma pequena taxa, desconfie.\n\n" +
                    "Os sinais mais comuns são ligações com endereços estranhos, urgência exagerada e pedidos de dados de cartão. As transportadoras não pedem pagamentos por SMS com ligações.\n\n" +
                    "Não carregue na ligação. Se já introduziu dados do cartão, contacte de imediato o seu banco para o bloquear.",
                category = "Alertas",
                publishedOn = daysAgo(8),
                imageUrl = null,
                url = null,
                membersOnly = false,
                isSample = true,
            ),
            Article(
                id = "sample-garantia",
                title = "Garantia de bens: três anos para reclamar de defeitos",
                summary = "Nos bens comprados a partir de 2022, o prazo legal de garantia é de três anos. Saiba o que pode exigir ao vendedor.",
                body = "Desde 2022, os bens móveis novos têm uma garantia legal de três anos.\n\n" +
                    "Se o produto apresentar um defeito, pode pedir a reparação ou a substituição. Em certos casos, pode também pedir a redução do preço ou a devolução do dinheiro.\n\n" +
                    "A garantia é responsabilidade do vendedor, mesmo que ele o encaminhe para o fabricante. Guarde a fatura ou o talão de compra.",
                category = "Direitos",
                publishedOn = daysAgo(12),
                imageUrl = null,
                url = null,
                membersOnly = false,
                isSample = true,
            ),
        )
    }
}
