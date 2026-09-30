package com.example.decosocio.data.demo

import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.AddOnKind
import com.example.decosocio.domain.model.AddOnRules
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.Address
import com.example.decosocio.domain.model.BillingPeriod
import com.example.decosocio.domain.model.ConsentRules
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.model.Plan
import com.example.decosocio.domain.model.PointsEntry
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.repository.DeletionReceipt
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random

/** Everything the demo backend knows about the single demo member. */
data class DemoState(
    val profile: MemberProfile,
    val consents: Consents,
    val addOns: List<AddOn>,
    val loyalty: LoyaltyAccount,
    val rewards: List<Reward>,
    val campaigns: List<CouponCampaign>,
    val coupons: List<Coupon>,
    /** Single-use code pools per campaign; a claimed code is removed from its pool. */
    val codePools: Map<String, List<String>>,
    val deletion: DeletionReceipt?,
)

/**
 * Fictitious demo data. Names, prices, partners and codes are invented examples,
 * not DECO PROteste's real offer.
 */
object DemoSeed {
    const val DEMO_EMAIL = "demo@exemplo.pt"
    const val DEMO_PASSWORD = "demo1234"
    const val CONTACT_KEY = "DEMO-0001"

    private fun LocalDate.plusDays(days: Int) = plus(DatePeriod(days = days))
    private fun LocalDate.minusDays(days: Int) = minus(DatePeriod(days = days))

    val plan = Plan(code = "COMPLETA", name = "Assinatura Completa (exemplo)", priceCents = 1290, period = BillingPeriod.MONTHLY)

    fun subscription(scenario: SubscriptionScenario, today: LocalDate): Subscription = when (scenario) {
        SubscriptionScenario.ACTIVE -> Subscription(plan, LocalDate(2023, 3, 1), today.plusDays(200), autoRenew = true, paymentMethodLabel = "Débito direto SEPA ···· 4321")
        SubscriptionScenario.EXPIRING -> Subscription(plan, LocalDate(2023, 3, 1), today.plusDays(12), autoRenew = false, paymentMethodLabel = "Multibanco (referência)")
        SubscriptionScenario.EXPIRED -> Subscription(plan, LocalDate(2023, 3, 1), today.minusDays(5), autoRenew = false, paymentMethodLabel = "Multibanco (referência)")
    }

    fun initial(today: LocalDate, random: Random = Random.Default): DemoState {
        val recentlyActivated = today.minusDays(5)
        val longAgo = today.minusDays(60)
        return DemoState(
            profile = MemberProfile(
                contactKey = CONTACT_KEY,
                memberNumber = "S-0012345",
                firstName = "Ana",
                lastName = "Ribeiro",
                email = DEMO_EMAIL,
                phone = "+351 912 345 678",
                address = Address("Rua do Exemplo, 10, 2.º Esq.", "1000-001", "Lisboa"),
                nif = null,
                preferredLanguage = "pt-PT",
            ),
            consents = ConsentRules.defaults(),
            addOns = listOf(
                AddOn(
                    id = "revista-digital",
                    name = "Revista digital de finanças",
                    description = "Edição mensal em formato digital com guias de poupança e investimento.",
                    priceCents = 299,
                    period = BillingPeriod.MONTHLY,
                    kind = AddOnKind.DIGITAL,
                    state = AddOnState.Available,
                ),
                AddOn(
                    id = "apoio-juridico",
                    name = "Apoio jurídico telefónico",
                    description = "Consulta individual com um jurista sobre problemas de consumo.",
                    priceCents = 499,
                    period = BillingPeriod.MONTHLY,
                    kind = AddOnKind.ADVICE,
                    state = AddOnState.Active(recentlyActivated, recentlyActivated.plusDays(AddOnRules.WITHDRAWAL_DAYS)),
                ),
                AddOn(
                    id = "seguro-compras",
                    name = "Seguro de proteção de compras",
                    description = "Cobertura de danos acidentais em compras até 1 000 € por ano.",
                    priceCents = 349,
                    period = BillingPeriod.MONTHLY,
                    kind = AddOnKind.INSURANCE,
                    state = AddOnState.Active(longAgo, longAgo.plusDays(AddOnRules.WITHDRAWAL_DAYS)),
                ),
                AddOn(
                    id = "analise-energia",
                    name = "Análise anual da fatura de energia",
                    description = "Revisão da sua fatura de eletricidade e gás com recomendação de tarifário.",
                    priceCents = 1990,
                    period = BillingPeriod.YEARLY,
                    kind = AddOnKind.SERVICE,
                    state = AddOnState.Available,
                ),
            ),
            loyalty = LoyaltyAccount(
                points = 820,
                lifetimePoints = 1240,
                history = listOf(
                    PointsEntry(today.minusDays(3), 120, "Participação num teste de produto"),
                    PointsEntry(today.minusDays(20), -200, "Vale de 5 € em combustível"),
                    PointsEntry(today.minusDays(45), 50, "Resposta a um inquérito"),
                    PointsEntry(today.minusDays(210), 300, "Renovação da assinatura"),
                ),
            ),
            rewards = listOf(
                Reward("r-cinema", "Bilhete de cinema 2x1", "Dois bilhetes pelo preço de um, de segunda a quinta.", "Cinemas Parceiros (exemplo)", 300, "2x1", 60),
                Reward("r-livros", "Vale de 10 € em livros", "Desconto numa compra de livros a partir de 30 €.", "Livraria Parceira (exemplo)", 600, "10 €", 90),
                Reward("r-oficina", "Revisão automóvel com 20% de desconto", "Válido numa revisão completa em oficinas aderentes.", "Rede de Oficinas (exemplo)", 900, "-20%", 60),
            ),
            campaigns = listOf(
                CouponCampaign("c-eletro", "10% em pequenos eletrodomésticos", "Código único, válido numa compra online.", "Loja Parceira (exemplo)", "-10%", today.plusDays(60), remainingCodes = 25),
                CouponCampaign("c-saude", "15% em produtos de saúde", "Último código disponível nesta campanha.", "Farmácia Parceira (exemplo)", "-15%", today.plusDays(30), remainingCodes = 1),
                CouponCampaign("c-verao", "Campanha de verão", "Campanha já terminada.", "Parceiro (exemplo)", "-5%", today.minusDays(1), remainingCodes = 10),
            ),
            coupons = listOf(
                Coupon("cp-1", null, "r-combustivel", "Vale de 5 € em combustível", "Posto Parceiro (exemplo)", "5 €", generateCode("FUEL", random), today.minusDays(20), today.plusDays(10), usedOn = null),
                Coupon("cp-0", null, "r-cinema", "Bilhete de cinema 2x1", "Cinemas Parceiros (exemplo)", "2x1", generateCode("CINE", random), today.minusDays(120), today.minusDays(60), usedOn = null),
            ),
            codePools = mapOf(
                "c-eletro" to List(25) { generateCode("ELEC", random) },
                "c-saude" to List(1) { generateCode("SAUDE", random) },
                "c-verao" to List(10) { generateCode("VERAO", random) },
            ),
            deletion = null,
        )
    }

    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateCode(prefix: String, random: Random = Random.Default): String {
        fun block() = (1..4).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
        return "$prefix-${block()}-${block()}"
    }
}
