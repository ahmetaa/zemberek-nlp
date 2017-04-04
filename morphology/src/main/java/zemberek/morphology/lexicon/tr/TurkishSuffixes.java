package zemberek.morphology.lexicon.tr;

import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.*;
import zemberek.morphology.lexicon.graph.DynamicSuffixProvider;
import zemberek.morphology.lexicon.graph.SuffixData;
import zemberek.morphology.lexicon.graph.TerminationType;

public class TurkishSuffixes extends DynamicSuffixProvider {

    // ------------ case suffixes ---------------------------

    public Suffix Dat = new Suffix("Dat");
    public SuffixForm Dat_yA = getForm(Dat, "+yA");
    public SuffixForm Dat_nA = getForm(Dat, "nA");

    public Suffix Loc = new Suffix("Loc");
    public SuffixForm Loc_dA = getForm(Loc, ">dA");
    public SuffixForm Loc_ndA = getForm(Loc, "ndA");

    public Suffix Abl = new Suffix("Abl");
    public SuffixForm Abl_dAn = getForm(Abl, ">dAn");
    public SuffixForm Abl_ndAn = getForm(Abl, "ndAn");

    public Suffix Gen = new Suffix("Gen");
    public SuffixForm Gen_nIn = getForm(Gen, "+nIn");
    public SuffixForm Gen_yIn = getForm(Gen, "+yIn"); //su-yun
    public SuffixForm Gen_Im = getForm("Gen_Im", Gen, "+Im"); // benim, bizim

    public Suffix Acc = new Suffix("Acc");
    public SuffixForm Acc_yI = getForm(Acc, "+yI");
    public SuffixForm Acc_nI = getForm(Acc, "nI");

    public Suffix Inst = new Suffix("Inst");
    public SuffixForm Inst_ylA = getForm(Inst, "+ylA");

    public Suffix Nom = new Suffix("Nom");
    public SuffixFormTemplate Nom_TEMPLATE = getTemplate("Nom_TEMPLATE", Nom);

    // ----------------- possesive ----------------------------

    public Suffix Pnon = new Suffix("Pnon");
    public SuffixFormTemplate Pnon_TEMPLATE = getTemplate("Pnon_TEMPLATE", Pnon);

    public Suffix P1sg = new Suffix("P1sg");
    public SuffixForm P1sg_Im = getForm(P1sg, "Im");
    public SuffixForm P1sg_yIm = getForm(P1sg, "+yIm"); //su-yum

    public Suffix P2sg = new Suffix("P2sg");
    public SuffixForm P2sg_In = getForm(P2sg, "In");
    public SuffixForm P2sg_yIn = getForm(P2sg, "+yIn"); //su-yun

    public Suffix P3sg = new Suffix("P3sg");
    public SuffixForm P3sg_sI = getForm(P3sg, "+sI");
    public SuffixForm P3sg_yI = getForm(P3sg, "+yI"); //su-yu

    public Suffix P1pl = new Suffix("P1pl");
    public SuffixForm P1pl_ImIz = getForm(P1pl, "ImIz");
    public SuffixForm P1pl_yImIz = getForm(P1pl, "+yImIz"); //su-yumuz

    public Suffix P2pl = new Suffix("P2pl");
    public SuffixForm P2pl_InIz = getForm(P2pl, "InIz");
    public SuffixForm P2pl_yInIz = getForm(P2pl, "+yInIz"); // su-yunuz

    public Suffix P3pl = new Suffix("P3pl");
    public SuffixForm P3pl_lArI = getForm(P3pl, "lArI");
    public SuffixForm P3pl_I = getForm(P3pl, "I"); // araba-lar-ı

    // -------------- Number-Person agreement --------------------

    public Suffix A1sg = new Suffix("A1sg");
    public SuffixForm A1sg_yIm = getForm(A1sg, "+yIm"); // gel-e-yim
    public SuffixForm A1sg_m = getForm(A1sg, "m"); // gel-se-m
    public SuffixFormTemplate A1sg_TEMPLATE = getTemplate("A1sg_TEMPLATE", A1sg); // ben

    public Suffix A2sg = new Suffix("A2sg");
    public SuffixForm A2sg_sIn = getForm(A2sg, "sIn"); // gel-ecek-sin
    public SuffixForm A2sg_n = getForm(A2sg, "n"); // gel-di-n
    public SuffixFormTemplate A2sg_TEMPLATE = getTemplate("A2sg_TEMPLATE", A2sg); // gel, sen,..
    public SuffixForm A2sg2_sAnA = getForm(A2sg, "sAnA"); //gel-sene
    public SuffixForm A2sg3_yInIz = getForm(A2sg, "+yInIz"); //gel-iniz

    public Suffix A3sg = new Suffix("A3sg");
    public SuffixFormTemplate A3sg_TEMPLATE = getTemplate("A3sg_TEMPLATE", A3sg); // gel-di-, o-
    public SuffixFormTemplate A3sg_Verb_TEMPLATE = getTemplate("A3sg_Verb_TEMPLATE", A3sg); // gel-di-, o-
    public SuffixForm A3sg_sIn = getForm(A3sg, "sIn"); // gel-sin

    public Suffix A1pl = new Suffix("A1pl");
    public SuffixForm A1pl_yIz = getForm(A1pl, "+yIz"); // geliyor-uz
    public SuffixForm A1pl_k = getForm(A1pl, "k"); // gel-di-k
    public SuffixForm A1pl_lIm = getForm(A1pl, "lIm"); // gel-e-lim
    public SuffixForm A1pl_ler = getForm(A1pl, "ler"); // biz-ler
    public SuffixFormTemplate A1pl_TEMPLATE = getTemplate("A1pl_TEMPLATE", A1pl); // biz

    public Suffix A2pl = new Suffix("A2pl");
    public SuffixForm A2pl_sInIz = getForm(A2pl, "sInIz"); // gel-ecek-siniz
    public SuffixForm A2pl_nIz = getForm(A2pl, "nIz"); // gel-di-niz
    public SuffixForm A2pl_yIn = getForm(A2pl, "+yIn"); // gel-me-yin
    public SuffixForm A2pl_ler = getForm(A2pl, "ler"); // siz-ler
    public SuffixFormTemplate A2pl_TEMPLATE = getTemplate("A2pl_TEMPLATE", A2pl); // gel-e-lim

    public Suffix A2pl2 = new Suffix("A2pl2");
    public SuffixForm A2pl2_sAnIzA = getForm(A2pl2, "sAnIzA"); // gel-senize

    public Suffix A3pl = new Suffix("A3pl");
    public SuffixForm A3pl_lAr = getForm(A3pl, "lAr"); // ev-ler
    public SuffixForm A3pl_Verb_lAr_After_Tense = getForm("A3pl_Verb_lAr_After_Tense", A3pl, "lAr"); // gel-ecek-ler
    public SuffixForm A3pl_Verb_lAr = getForm("A3pl_Verb_lAr", A3pl, "lAr"); // gel-ecek-ler
    public SuffixForm A3pl_Comp_lAr = getForm("A3pl_Comp_lAr", A3pl, "lAr", TerminationType.NON_TERMINAL); //zeytinyağlarımız
    public SuffixForm A3pl_sInlAr = getForm(A3pl, "sInlAr"); // gel-sinler
    public SuffixForm A3pl_nlAr = getForm(A3pl, "nlAr"); // bu-nlar

    // ------------ derivatioonal ----------------------

    public Suffix Dim = new Suffix("Dim");
    public SuffixForm Dim_cIk = getForm(Dim, ">cI~k");

    public Suffix Dim2 = new Suffix("Dim2");
    public SuffixForm Dim2_cAgIz = getForm(Dim2, "cAğIz");

    public Suffix With = new Suffix("With");
    public SuffixForm With_lI = getForm(With, "lI");

    public Suffix Without = new Suffix("Without");
    public SuffixForm Without_sIz = getForm(Without, "sIz");

    public Suffix Rel = new Suffix("Rel");
    public SuffixForm Rel_ki = getForm(Rel, "ki"); // masa-da-ki
    public SuffixForm Rel_kI = getForm(Rel, "kI"); // dünkü

    public Suffix Agt = new Suffix("Agt");
    public SuffixForm Agt_cI = getForm(Agt, ">cI"); // araba-cı. Converts to another Noun.
    public SuffixForm Agt_yIcI_2Noun = getForm("Agt_yIcI_2Noun", Agt, "+yIcI"); // otur-ucu. converts to both Noun and Adj
    public SuffixForm Agt_yIcI_2Adj = getForm("Agt_yIcI_2Adj", Agt, "+yIcI"); // otur-ucu. converts to both Noun and Adj

    public Suffix Ness = new Suffix("Ness");
    public SuffixForm Ness_lIk = getForm(Ness, "lI~k");

    public Suffix FitFor = new Suffix("FitFor");
    public SuffixForm FitFor_lIk = getForm(FitFor, "lI~k");


    public Suffix Become = new Suffix("Become");
    public SuffixForm Become_lAs = getForm(Become, "lAş");
    public SuffixForm Become_Adj_lAs = getForm("Become_Adj_lAs", Become, "lAş");

    public Suffix Acquire = new Suffix("Acquire");
    public SuffixForm Acquire_lAn = getForm(Acquire, "lAn");

    public Suffix JustLike = new Suffix("JustLike");
    public SuffixForm JustLike_ImsI = getForm(JustLike, "ImsI"); // udunumsu
    public SuffixForm JustLike_msI = getForm(JustLike, "+msI"); // odunsu

    public SuffixForm JustLike_Adj_ImsI = getForm("JustLike_Adj_ImsI", JustLike, "ImsI"); // udunumsu
    public SuffixForm JustLike_Adj_msI = getForm("JustLike_Adj_msI", JustLike, "+msI"); // odunsu

    public Suffix Related = new Suffix("Related");
    public SuffixForm Related_sAl = getForm(Related, "sAl");

    // ----------------------------  verbal tense --------------------------------

    public Suffix Aor = new Suffix("Aor");
    public SuffixForm Aor_Ir = getForm(Aor, "+Ir"); //gel-ir
    public SuffixForm Aor_Ar = getForm(Aor, "+Ar"); //ser-er
    public SuffixForm Aor_z = getForm(Aor, "z"); // gel-me-z
    public SuffixFormTemplate Aor_EMPTY = getTemplate("Aor_EMPTY", Aor, TerminationType.NON_TERMINAL); // gel-me--yiz

    public Suffix Prog = new Suffix("Prog");
    public SuffixForm Prog_Iyor = getForm(Prog, "Iyor");

    public Suffix Prog2 = new Suffix("Prog2");
    public SuffixForm Prog2_mAktA = getForm(Prog2, "mAktA");

    public Suffix Fut = new Suffix("Fut");
    public SuffixForm Fut_yAcAk = getForm(Fut, "+yAcA~k");

    public Suffix Past = new Suffix("Past");
    public SuffixForm Past_dI = getForm(Past, ">dI");

    public Suffix Narr = new Suffix("Narr");
    public SuffixForm Narr_mIs = getForm(Narr, "mIş");

    // ---------------------------------------------------

    public Suffix PastPart = new Suffix("PastPart");
    public SuffixForm PastPart_dIk_2Noun = getForm("PastPart_dIk_2Noun", PastPart, ">dI~k");
    public SuffixForm PastPart_dIk_2Adj = getForm("PastPart_dIk_2Noun", PastPart, ">dI~k");

    public Suffix AorPart = new Suffix("AorPart"); // convert to an Adjective
    public SuffixForm AorPart_Ir_2Adj = getForm("AorPart_Ir_2Adj", AorPart, "+Ir"); //gel-ir
    public SuffixForm AorPart_Ar_2Adj = getForm("AorPart_Ar_2Adj", AorPart, "+Ar"); //ser-er
    public SuffixForm AorPart_z_2Adj = getForm("AorPart_z_2Adj", AorPart, "z"); // gel-me-z.

    public Suffix FutPart = new Suffix("FutPart");
    public SuffixForm FutPart_yAcAk_2Adj = getForm("FutPart_yAcAk_2Adj", FutPart, "+yAcA~k");
    public SuffixForm FutPart_yAcAk_2Noun = getForm("FutPart_yAcAk_2Noun", FutPart, "+yAcA~k", TerminationType.NON_TERMINAL); // if it is at the end, FutPart only converts to Adj

    public Suffix NarrPart = new Suffix("NarrPart");
    public SuffixForm NarrPart_mIs_2Adj = getForm("NarrPart_mIs_2Adj", NarrPart, "mIş");
    public SuffixForm NarrPart_mIs_2Noun = getForm("NarrPart_mIs_2Noun", NarrPart, "mIş", TerminationType.NON_TERMINAL);

    public Suffix PresPart = new Suffix("PresPart");
    public SuffixForm PresPart_yAn = getForm(PresPart, "+yAn");

    public Suffix Pos = new Suffix("Pos");
    public SuffixFormTemplate Pos_EMPTY = getTemplate("Pos_EMPTY", Pos); // Verb Positive Null Morpheme template.

    public Suffix Neg = new Suffix("Neg");
    public SuffixForm Neg_mA = getForm(Neg, "mA"); //gel-me
    public SuffixForm Neg_m = getForm("Neg_m", Neg, "m", TerminationType.NON_TERMINAL); // gel-m-iyor

    //TODO: May be redundant. Cond_Cop may suffice
    public Suffix Cond = new Suffix("Cond");
    public SuffixForm Cond_sA = getForm(Cond, "sA");
    public SuffixForm Cond_sA_AfterPerson = getForm(Cond, "sA");

    public Suffix Necess = new Suffix("Necess");
    public SuffixForm Necess_mAlI = getForm(Necess, "mAlI");

    public Suffix Opt = new Suffix("Opt");
    public SuffixForm Opt_yA = getForm(Opt, "+yA");

    public Suffix Pass = new Suffix("Pass");
    public SuffixForm Pass_In = getForm(Pass, "+In");
    public SuffixForm Pass_InIl = getForm(Pass, "+InIl");
    public SuffixForm Pass_nIl = getForm(Pass, "+nIl");

    public Suffix Caus = new Suffix("Caus");
    public SuffixForm Caus_t = getForm(Caus, "t");
    public SuffixForm Caus_tIr = getForm(Caus, ">dIr");

    public Suffix Imp = new Suffix("Imp");
    public SuffixFormTemplate Imp_TEMPLATE = getTemplate("Imp_TEMPLATE", Imp);

    public Suffix Des = new Suffix("Des");
    public SuffixForm Des_sA = getForm(Des, "sA");

/*    public Suffix Recip = new Suffix("Recip");
    public SuffixForm Recip_Is = getForm(Recip, "+Iş");
    public SuffixForm Recip_yIs = getForm(Recip, "+yIş");*/

    public Suffix Reflex = new Suffix("Reflex");
    public SuffixForm Reflex_In = getForm(Reflex, "+In");

    public Suffix Abil = new Suffix("Abil");
    public SuffixForm Abil_yAbil = getForm(Abil, "+yAbil");
    public SuffixForm Abil_yA = getForm("Abil_yA", Abil, "+yA", TerminationType.NON_TERMINAL);

    public Suffix Cop = new Suffix("Cop");
    public SuffixForm Cop_dIr = getForm(Cop, ">dIr");

    public SuffixForm PastCop_ydI = getForm("PastCop_ydI", Past, "+y>dI");

    public SuffixForm NarrCop_ymIs = getForm("NarrCop_ymIs", Narr, "+ymIş");

    public SuffixForm CondCop_ysA = getForm("CondCop_ysA", Cond, "+ysA");

    public Suffix While = new Suffix("While");
    public SuffixForm While_ken = getForm(While, "+yken");

    public Suffix Pres = new Suffix("Pres");  // Present tense only appears after a zero morpheme verb derivation such as "kalemdir"
    public SuffixFormTemplate Pres_TEMPLATE = getTemplate("Pres_TEMPLATE", Pres);

    public Suffix Equ = new Suffix("Equ");
    public SuffixForm Equ_cA = getForm(Equ, ">cA");
    public SuffixForm Equ_ncA = getForm(Equ, "ncA");

    public Suffix NotState = new Suffix("NotState");
    public SuffixForm NotState_mAzlIk = getForm(NotState, "mAzlI~k");

    public Suffix ActOf = new Suffix("ActOf");
    public SuffixForm ActOf_mAcA = getForm(ActOf, "mAcA");

    public Suffix AsIf = new Suffix("AsIf");
    public SuffixForm AsIf_cAsInA = getForm(AsIf, ">cAsInA");

    // Converts to an Adverb.
    public Suffix AsLongAs = new Suffix("AsLongAs");
    public SuffixForm AsLongAs_dIkcA = getForm(AsLongAs, ">dIkçA");

    public Suffix When = new Suffix("When");
    public SuffixForm When_yIncA = getForm(When, "+yIncA");

    // It also may have "worthy of doing" meaning after passive. Converts to an Adjective.
    public Suffix FeelLike = new Suffix("FeelLike");
    public SuffixForm FeelLike_yAsI_2Noun = getForm("FeelLike_yAsI_2Noun", FeelLike, "+yAsI");
    public SuffixForm FeelLike_yAsI_2Adj = getForm("FeelLike_yAsI_2Adj", FeelLike, "+yAsI");

    // Converts to an Adverb.
    public Suffix SinceDoing = new Suffix("SinceDoing");
    public SuffixForm SinceDoing_yAlI = getForm(SinceDoing, "+yAlI");

    // Converts to an Adverb.
    public Suffix ByDoing = new Suffix("ByDoing");
    public SuffixForm ByDoing_yArAk = getForm(ByDoing, "+yArAk");

    // Converts to an Adverb.
    // TODO: this should have a Neg_null effect
    public Suffix WithoutDoing = new Suffix("WithoutDoing");
    public SuffixForm WithoutDoing_mAdAn = getForm(WithoutDoing, "mAdAn");

    // Converts to an Adverb.
    public Suffix UntilDoing = new Suffix("UntilDoing");
    public SuffixForm UntilDoing_yAsIyA = getForm(UntilDoing, "+yAsIyA");


    public Suffix WithoutDoing2 = new Suffix("WithoutDoing2");
    public SuffixForm WithoutDoing2_mAksIzIn = getForm(WithoutDoing2, "mAksIzIn");

    // Converts to an Adverb.
    public Suffix AfterDoing = new Suffix("AfterDoing");
    public SuffixForm AfterDoing_yIp = getForm(AfterDoing, "+yIp");

    public Suffix UnableToDo = new Suffix("UnableToDo");
    public SuffixForm UnableToDo_yAmAdAn = getForm(UnableToDo, "+yAmAdAn");

    public Suffix InsteadOfDoing = new Suffix("InsteadOfDoing");
    public SuffixForm InsteadOfDoing_mAktAnsA = getForm(InsteadOfDoing, "mAktAnsA");

    // Converts to an Adverb.
    public Suffix KeepDoing = new Suffix("KeepDoing");
    public SuffixForm KeepDoing_yAgor = getForm(KeepDoing, "+yAgör");

    public Suffix KeepDoing2 = new Suffix("KeepDoing2");
    public SuffixForm KeepDoing2_yAdur = getForm(KeepDoing2, "+yAdur");

    public Suffix EverSince = new Suffix("EverSince");
    public SuffixForm EverSince_yAgel = getForm(EverSince, "+yAgel");

    public Suffix Almost = new Suffix("Almost");
    public SuffixForm Almost_yAyAz = getForm(Almost, "+yAyaz");

    public Suffix Hastily = new Suffix("Hastily");
    public SuffixForm Hastily_yIver = getForm(Hastily, "+yIver");

    public Suffix Stay = new Suffix("Stay");
    public SuffixForm Stay_yAkal = getForm(Stay, "+yAkal");

    public Suffix Start = new Suffix("Start");
    public SuffixForm Start_yAkoy = getForm(Start, "+yAkoy");

    public Suffix Inf1 = new Suffix("Inf1");
    public SuffixForm Inf1_mAk = getForm(Inf1, "mAk");

    //TODO: consider -maca inf suffix.
    //public SuffixForm Inf1_mAcA = getForm(Inf1, "mAcA");

    public Suffix Inf2 = new Suffix("Inf2");
    public SuffixForm Inf2_mA = getForm(Inf2, "mA");

    public Suffix Inf3 = new Suffix("Inf3");
    public SuffixForm Inf3_yIs = getForm(Inf3, "+yIş");

    // TODO is below a valid morpheme? 
/*    public Suffix NounDeriv = new Suffix("NounDeriv");
    public SuffixForm NounDeriv_nIm = getForm(NounDeriv, "+nIm");*/

    public Suffix Ly = new Suffix("Ly");
    public SuffixForm Ly_cA = getForm(Ly, ">cA");

    public Suffix Quite = new Suffix("Quite");
    public SuffixForm Quite_cA = getForm(Quite, ">cA");

    public Suffix Ordinal = new Suffix("Ordinal");
    public SuffixForm Ordinal_IncI = getForm(Ordinal, "+IncI");

    public Suffix Grouping = new Suffix("Grouping");
    public SuffixForm Grouping_sAr = getForm(Grouping, "+şAr");

    public RootSuffix NounRoot = new RootSuffix("Noun", PrimaryPos.Noun);
    public SuffixFormTemplate Noun_TEMPLATE = getTemplate("Noun_TEMPLATE", NounRoot);
    public DerivationalSuffixTemplate Noun2Noun = getDerivationalTemplate("Noun2Noun", NounRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Adj2Noun = getDerivationalTemplate("Adj2Noun", NounRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Adv2Noun = getDerivationalTemplate("Adv2Noun", NounRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2Noun = getDerivationalTemplate("Verb2Noun", NounRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Num2Noun = getDerivationalTemplate("Num2Noun", NounRoot, TerminationType.NON_TERMINAL);

    public DerivationalSuffixTemplate Verb2NounPart = getDerivationalTemplate("Verb2NounPart", NounRoot, TerminationType.NON_TERMINAL);

    public NullSuffixForm Noun_Default = getNull("Noun_Default", Noun_TEMPLATE);
    public NullSuffixForm Noun_Time_Default = getNull("Noun_Time_Default", Noun_TEMPLATE);
    public NullSuffixForm Noun_Comp_P3sg = getNull("Noun_Comp_P3sg", Noun_TEMPLATE);
    public NullSuffixForm Noun_Comp_P3sg_Root = getNull("Noun_Comp_P3sg_Root", Noun_TEMPLATE);
    public NullSuffixForm Noun_Su_Root = getNull("Noun_Su_Root", Noun_TEMPLATE);

    public RootSuffix AdjRoot = new RootSuffix("Adj", PrimaryPos.Adjective);
    public SuffixFormTemplate Adj_TEMPLATE = getTemplate("Adj_TEMPLATE", AdjRoot);
    public DerivationalSuffixTemplate Noun2Adj = getDerivationalTemplate("Noun2Adj", AdjRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Adj2Adj = getDerivationalTemplate("Adj2Adj", AdjRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Adv2Adj = getDerivationalTemplate("Adv2Adj", AdjRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2Adj = getDerivationalTemplate("Verb2Adj", AdjRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2AdjPart = getDerivationalTemplate("Verb2AdjPart", AdjRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Num2Adj = getDerivationalTemplate("Num2Adj", AdjRoot, TerminationType.NON_TERMINAL);

    public SuffixFormTemplate Adj_Main_Rel = getTemplate("Adj_TEMPLATE", AdjRoot);
    public NullSuffixForm Adj_Default = getNull("Adj_Default", Adj_TEMPLATE, TerminationType.TERMINAL);

    public RootSuffix AdvRoot = new RootSuffix("Adv", PrimaryPos.Adverb);

    public SuffixFormTemplate Adv_TEMPLATE = getTemplate("Adv_TEMPLATE", AdvRoot);
    public DerivationalSuffixTemplate Adj2Adv = getDerivationalTemplate("Adj2Adv", AdvRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2Adv = getDerivationalTemplate("Verb2Adv", AdvRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Num2Adv = getDerivationalTemplate("Num2Adv", AdvRoot, TerminationType.NON_TERMINAL);
    public NullSuffixForm Adv_Default = getNull("Adv_Default", Adv_TEMPLATE);

    public RootSuffix InterjRoot = new RootSuffix("Interj", PrimaryPos.Interjection);
    public SuffixFormTemplate Interj_Template = getTemplate("Interj_Template", InterjRoot);
    public NullSuffixForm Interj_Default = getNull("Interj_Default", Interj_Template);

    public RootSuffix PuncRoot = new RootSuffix("Punc", PrimaryPos.Punctuation);
    public SuffixFormTemplate Punc_Template = getTemplate("Punc_Template", PuncRoot);
    public NullSuffixForm Punc_Default = getNull("Punc_Default", Punc_Template);

    public RootSuffix ConjRoot = new RootSuffix("Conj", PrimaryPos.Conjunction);
    public SuffixFormTemplate Conj_Template = getTemplate("Conj_Template", ConjRoot);
    public NullSuffixForm Conj_Default = getNull("Conj_Default", Conj_Template);

    public RootSuffix NumeralRoot = new RootSuffix("Numeral", PrimaryPos.Numeral);
    public SuffixFormTemplate Numeral_Template = getTemplate("Numeral_Template", NumeralRoot);
    public NullSuffixForm Numeral_Default = getNull("Numeral_Default", Numeral_Template);

    public RootSuffix DetRoot = new RootSuffix("Det", PrimaryPos.Determiner);
    public SuffixFormTemplate Det_Template = getTemplate("Det_Template", DetRoot);
    public NullSuffixForm Det_Default = getNull("Det_Default", Det_Template);

    public RootSuffix PostpRoot = new RootSuffix("Postp", PrimaryPos.PostPositive);
    public SuffixFormTemplate Postp_Template = getTemplate("Postp_Template", PostpRoot);
    public NullSuffixForm Postp_Default = getNull("Postp_Default", Postp_Template);
    public DerivationalSuffixTemplate Postp2Noun = getDerivationalTemplate("Postp2Noun", NounRoot, TerminationType.NON_TERMINAL);

    public RootSuffix DupRoot = new RootSuffix("DupRoot", PrimaryPos.Duplicator);
    public SuffixFormTemplate Dup_Template = getTemplate("Dup_Template", DupRoot);
    public NullSuffixForm Dup_Default = getNull("Dup_Default", Dup_Template);

    public SuffixFormTemplate ProperNoun_Template = getTemplate("ProperNoun_Template", NounRoot);
    public NullSuffixForm ProperNoun_Default = getNull("ProperNoun_Default", ProperNoun_Template);

    public RootSuffix VerbRoot = new RootSuffix("Verb", PrimaryPos.Verb);
    public SuffixFormTemplate Verb_TEMPLATE = getTemplate("Verb_TEMPLATE", VerbRoot);
    public DerivationalSuffixTemplate Adj2Verb = getDerivationalTemplate("Adj2Verb", VerbRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Noun2Verb = getDerivationalTemplate("Noun2Verb", VerbRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Noun2VerbCopular = getDerivationalTemplate("Noun2VerbCopular", VerbRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2Verb = getDerivationalTemplate("Verb2Verb", VerbRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2VerbAbility = getDerivationalTemplate("Verb2VerbAbility", VerbRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Verb2VerbCompounds = getDerivationalTemplate("Verb2VerbCompounds", VerbRoot, TerminationType.NON_TERMINAL);
    public DerivationalSuffixTemplate Num2Verb = getDerivationalTemplate("Num2Verb", VerbRoot, TerminationType.NON_TERMINAL);

    public NullSuffixForm Verb_Default = getNull("Verb_Default", Verb_TEMPLATE);
    //    public NullSuffixForm Verb_Reciprocal = getNull("Verb_Default", Verb_TEMPLATE);
    public NullSuffixForm Verb_De = getNull("Verb_De", Verb_TEMPLATE);
    public NullSuffixForm Verb_Di = getNull("Verb_Di", Verb_TEMPLATE);
    public NullSuffixForm Verb_Ye = getNull("Verb_Ye", Verb_TEMPLATE);
    public NullSuffixForm Verb_Yi = getNull("Verb_Yi", Verb_TEMPLATE);

    public SuffixForm Verb_De_Ye_Prog = getNull("Verb_De_Ye_Prog", Verb_TEMPLATE);
    public SuffixForm Verb_Prog_Drop = getNull("Verb_Prog_Drop", Verb_TEMPLATE);

    public RootSuffix PersPronRoot = new RootSuffix("PersPron", PrimaryPos.Pronoun);
    public RootSuffix DemonsPronRoot = new RootSuffix("DemonsPron", PrimaryPos.Pronoun);
    public RootSuffix QuantPronRoot = new RootSuffix("QuantPron", PrimaryPos.Pronoun);
    public RootSuffix QuesPronRoot = new RootSuffix("QuesPronRoot", PrimaryPos.Pronoun);
    public RootSuffix ReflexPronRoot = new RootSuffix("ReflexPronRoot", PrimaryPos.Pronoun);
    public SuffixFormTemplate PersPron_TEMPLATE = getTemplate("PersPron_TEMPLATE", PersPronRoot);
    public NullSuffixForm PersPron_Default = getNull("PersPron_Default", PersPron_TEMPLATE);
    public NullSuffixForm PersPron_Ben = getNull("PersPron_Ben", PersPron_TEMPLATE);
    public NullSuffixForm PersPron_Sen = getNull("PersPron_Sen", PersPron_TEMPLATE);
    public NullSuffixForm PersPron_O = getNull("PersPron_O", PersPron_TEMPLATE);
    public NullSuffixForm PersPron_Biz = getNull("PersPron_Biz", PersPron_TEMPLATE);
    public NullSuffixForm PersPron_Siz = getNull("PersPron_Siz", PersPron_TEMPLATE);

    public SuffixFormTemplate PersPron_BanSan = getTemplate("PersPron_BanSan", PersPronRoot);


    public DerivationalSuffixTemplate Pron2Verb = getDerivationalTemplate("Pron2Verb", VerbRoot, TerminationType.NON_TERMINAL);

    public SuffixFormTemplate DemonsPron_TEMPLATE = getTemplate("DemonsPron_TEMPLATE", DemonsPronRoot);
    public NullSuffixForm DemonsPron_Default = getNull("DemonsPron_Default", DemonsPron_TEMPLATE);

    public SuffixFormTemplate ReflexPron_TEMPLATE = getTemplate("ReflexPron_TEMPLATE", ReflexPronRoot);
    public NullSuffixForm ReflexPron_Default = getNull("ReflexPron_Default", ReflexPron_TEMPLATE);

    public SuffixFormTemplate QuantPron_TEMPLATE = getTemplate("QuantPron_TEMPLATE", QuantPronRoot);
    public NullSuffixForm QuantPron_Default = getNull("QuantPron_Default", QuantPron_TEMPLATE);

    public SuffixFormTemplate QuesPron_TEMPLATE = getTemplate("QuesPron_TEMPLATE", QuesPronRoot);
    public NullSuffixForm QuesPron_Default = getNull("QuesPron_Default", QuesPron_TEMPLATE);

    public RootSuffix QuesRoot = new RootSuffix("Ques", PrimaryPos.Question);
    public SuffixFormTemplate Ques_Template = getTemplate("Ques_Template", QuesRoot);
    public NullSuffixForm Ques_Default = getNull("Ques_Default", Ques_Template);

    // TODO: add time root. (with Rel_ki + Noun)
    public final SuffixForm[] CASE_FORMS = {Nom_TEMPLATE, Dat_yA, Loc_dA, Abl_dAn, Gen_nIn, Acc_yI, Inst_ylA, Equ_cA};

    public final SuffixForm[] POSSESSIVE_FORMS = {Pnon_TEMPLATE, P1sg_Im, P2sg_In, P3sg_sI, P1pl_ImIz, P2pl_InIz, P3pl_lArI};
    public final SuffixForm[] PERSON_FORMS_N = {A1sg_yIm, A2sg_sIn, A3sg_TEMPLATE, A1pl_yIz, A2pl_sInIz, A3pl_lAr};
    public final SuffixForm[] PERSON_FORMS_COP = {A1sg_m, A2sg_n, A3sg_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_lAr};
    public final SuffixForm[] COPULAR_FORMS = {Cop_dIr, PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken};
    public final SuffixForm[] COPULAR_PERSON_FORMS = {
            A1sg_m, A2sg_n, A3sg_Verb_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_Verb_lAr,
            A1sg_yIm, A2sg_sIn, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, Cop_dIr};


    public TurkishSuffixes() {

        //---------------------------- Root and Derivation Morphemes ---------------------------------------------------

        // noun template. it has all possible suffix forms that a noun can have
        Noun_TEMPLATE.connections.add(A3pl_lAr, A3pl_Comp_lAr, A3sg_TEMPLATE);
        Noun_TEMPLATE.indirectConnections
                .add(POSSESSIVE_FORMS, CASE_FORMS)
                .add(P1sg_yIm, P2sg_yIn, P3sg_yI, P1pl_yImIz, P2pl_yInIz, P3pl_I, Gen_yIn)
                .add(Cop_dIr, PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken)
                .add(A1sg_yIm, A2sg_sIn, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz)
                .add(A1sg_m, A2sg_n, A3sg_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_lAr)
                .add(Dat_nA, Loc_ndA, Abl_ndAn, Acc_nI, Equ_ncA)
                .add(Dim_cIk, Dim2_cAgIz, With_lI, Without_sIz, Agt_cI, JustLike_msI, JustLike_ImsI, Ness_lIk, Related_sAl, FitFor_lIk)
                .add(Become_lAs, Acquire_lAn, Pres_TEMPLATE)
                .add(Noun2Noun, Noun2Adj, Noun2Verb, Noun2VerbCopular);

        // default noun suffix form. we remove some suffixes so that words like araba-na (dative)
        Noun_Default.connections.add(A3pl_lAr, A3sg_TEMPLATE);
        Noun_Default.indirectConnections
                .add(Noun_TEMPLATE.indirectConnections)
                .remove(Dat_nA, Loc_ndA, Abl_ndAn, Acc_nI, Rel_kI)
                .remove(P1sg_yIm, P2sg_yIn, P3sg_yI, P1pl_yImIz, P2pl_yInIz, Gen_yIn);

        Noun_Time_Default.connections.add(Noun_Default.connections);
        Noun_Time_Default.indirectConnections.add(Noun_Default.indirectConnections)
                .add(Rel_kI);

        // TODO: check below
        DemonsPron_TEMPLATE.connections.add(A3sg_TEMPLATE, A3pl_nlAr, A1pl_TEMPLATE);
        DemonsPron_TEMPLATE.indirectConnections.add(With_lI, Inst_ylA, Without_sIz, Acc_nI, Dat_nA, Loc_ndA, Gen_nIn, Nom_TEMPLATE,
                Abl_ndAn, Cop_dIr, Pron2Verb, PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken, A3pl_lAr)
                .add(Pnon_TEMPLATE)
                .add(Noun2Verb, Noun2VerbCopular);
        DemonsPron_Default.copyConnections(DemonsPron_TEMPLATE);

        // TODO: birbiri, birbirimizi problematic
        QuantPron_Default.copyConnections(DemonsPron_TEMPLATE).connections.add(A3pl_lAr);
        QuantPron_Default.indirectConnections.add(P3sg_sI, P3pl_I, P1pl_ImIz).remove(P3sg_sI, P3sg_yI);

        PersPron_Default.copyConnections(DemonsPron_TEMPLATE);
        ReflexPron_Default.copyConnections(DemonsPron_TEMPLATE);
        ReflexPron_Default.indirectConnections.add(Dat_nA, P3sg_sI);
        ReflexPron_Default.connections.add(A2sg_TEMPLATE);

        Pron2Verb.connections.add(Noun2VerbCopular.connections);
        Pron2Verb.indirectConnections.add(Noun2VerbCopular.indirectConnections);


        Ques_Template.connections.add(A3sg_TEMPLATE, A3pl_lAr);
        Ques_Template.indirectConnections.add(Pron2Verb, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, Pnon_TEMPLATE,
                Abl_dAn, A1sg_yIm, A2sg_sIn);

        Ques_Default.copyConnections(Ques_Template);


        Numeral_Template.connections.add(Noun_TEMPLATE.connections);
        Numeral_Template.connections.add(Num2Noun, Num2Adj, Num2Adv, Num2Verb);
        Numeral_Template.indirectConnections.add(Noun_TEMPLATE.indirectConnections);

        Numeral_Default.connections.add(Num2Noun, Num2Adj, Num2Adv, Num2Verb);
        Numeral_Default.indirectConnections.add(Noun_TEMPLATE.allConnections());

        Noun2Noun.connections.add(Dim_cIk, Dim2_cAgIz, Agt_cI, Ness_lIk);
        // TODO: JustLike_msI, JustLike_ImsI may cause duplicate analysis results. See #54
        Noun2Adj.connections.add(With_lI, Without_sIz, JustLike_msI, JustLike_ImsI, Rel_ki, Rel_kI, Related_sAl, FitFor_lIk);

        Noun2Verb.connections.add(Become_lAs, Acquire_lAn);

        Noun2VerbCopular.connections.add(Pres_TEMPLATE, PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken);
        Noun2VerbCopular.indirectConnections.add(A1sg_m, A2sg_n, A3sg_Verb_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_Verb_lAr);
        Noun2VerbCopular.indirectConnections.add(A1sg_yIm, A2sg_sIn, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, Cop_dIr);

        Adj2Noun.connections.add(Noun_TEMPLATE.connections);
        Adj2Noun.indirectConnections.add(Noun_Default.indirectConnections).remove(/*Noun2VerbCopular,*/ FitFor_lIk, Related_sAl, Become_lAs, JustLike_ImsI, JustLike_msI, Equ_cA, Equ_ncA);

        //TODO: Too much suffix for Postp.
        Postp2Noun.connections.add(Noun_TEMPLATE.connections);
        Postp2Noun.indirectConnections.add(Noun_Default.indirectConnections).remove(Related_sAl, Become_lAs, JustLike_ImsI, JustLike_msI, Equ_cA, Equ_ncA);

        Adj2Adj.connections.add(Quite_cA, JustLike_Adj_ImsI, JustLike_Adj_msI);

        Adj2Adv.connections.add(Ly_cA);

        Adj2Verb.connections.add(Become_Adj_lAs, Acquire_lAn).add(COPULAR_FORMS);

        Num2Adj.connections.add(Quite_cA, JustLike_Adj_ImsI, JustLike_Adj_msI);
        Num2Noun.connections.add(Adj2Noun.connections);
        Num2Noun.indirectConnections.add(Adj2Noun.indirectConnections).remove(FitFor_lIk);
        Num2Verb.connections.add(Become_Adj_lAs, Acquire_lAn).add(COPULAR_FORMS);

        Adv2Noun.connections.add(A3sg_TEMPLATE);
        Adv2Noun.indirectConnections.add(Pnon_TEMPLATE, Dat_yA);

        Adv2Adj.connections.add(Rel_ki); // ararkenki

        Verb2Verb.connections.add(Caus_t, Caus_tIr, Pass_In, Pass_nIl, Pass_InIl, Abil_yA/*, Recip_Is, Recip_yIs*/);

        Verb2VerbCompounds.connections.add(KeepDoing_yAgor, KeepDoing2_yAdur, EverSince_yAgel, Almost_yAyAz, Hastily_yIver, Stay_yAkal, Start_yAkoy);

        Verb2VerbAbility.connections.add(Abil_yAbil);

        Verb2Noun.connections.add(Inf1_mAk, Inf2_mA, Inf3_yIs, FeelLike_yAsI_2Noun, Agt_yIcI_2Noun, ActOf_mAcA, NotState_mAzlIk);

        Verb2NounPart.connections.add(PastPart_dIk_2Noun, NarrPart_mIs_2Noun, FutPart_yAcAk_2Noun);

        Verb2AdjPart.connections.add(PastPart_dIk_2Adj, NarrPart_mIs_2Adj, FutPart_yAcAk_2Adj, AorPart_Ar_2Adj, AorPart_Ir_2Adj, AorPart_z_2Adj, PresPart_yAn);

        Verb2Adv.connections.add(When_yIncA, SinceDoing_yAlI, UnableToDo_yAmAdAn, ByDoing_yArAk,
                WithoutDoing_mAdAn, WithoutDoing2_mAksIzIn, While_ken)
                .add(InsteadOfDoing_mAktAnsA, AsLongAs_dIkcA, AfterDoing_yIp, AsIf_cAsInA);

        Verb2Adj.connections.add(When_yIncA, FeelLike_yAsI_2Adj, Agt_yIcI_2Adj);

        Adv_TEMPLATE.connections.add(Adv2Noun, Adv2Adj);

        Adj_TEMPLATE.connections.add(Adj2Noun, Adj2Adj, Adj2Adv, Adj2Verb);
        Adj_TEMPLATE.indirectConnections.add(
                Adj2Noun.allConnections(),
                Adj2Adj.allConnections(),
                Adj2Adv.allConnections(),
                Adj2Verb.allConnections());

        Postp_Template.connections.add(Postp2Noun);
        Postp_Template.indirectConnections.add(Postp2Noun.allConnections());

        Postp_Default.connections.add(Postp_Template.connections);
        Postp_Default.indirectConnections.add(Postp_Template.indirectConnections);

        Adj_Default.connections.add(Adj_TEMPLATE.connections);
        Adj_Default.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        // P3sg compound suffixes. (full form. such as zeytinyağı-na)
        Noun_Comp_P3sg.connections.add(A3sg_TEMPLATE);
        Noun_Comp_P3sg.indirectConnections
                //.add(POSSESSIVE_FORMS)
                .add(Pnon_TEMPLATE, Nom_TEMPLATE, Pres_TEMPLATE)
                .add(Noun2Noun, Noun2Adj, Noun2Verb, Noun2VerbCopular)
                .add(Dat_nA, Loc_ndA, Abl_ndAn, Gen_nIn, Acc_nI, Inst_ylA)
                .add(Cop_dIr, PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken)
                .add(A1sg_m, A2sg_n, A3sg_TEMPLATE, A3sg_Verb_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_lAr)
                .add(A1sg_yIm, A1pl_yIz, A2sg_sIn, A2pl_sInIz);

        // P3sg compound suffixes. (root form. such as zeytinyağ-lar-ı)
        Noun_Comp_P3sg_Root.connections.add(A3pl_Comp_lAr, A3sg_TEMPLATE); // A3pl_Comp_lAr is used, because zeytinyağ-lar is not allowed.
        Noun_Comp_P3sg_Root.indirectConnections
                .add(Pnon_TEMPLATE, Nom_TEMPLATE, With_lI, Without_sIz, Agt_cI, JustLike_msI, JustLike_ImsI, Ness_lIk, Related_sAl)
                .add(P3pl_lArI)
                .add(POSSESSIVE_FORMS)
                .add(Noun2Noun.allConnections())
                .add(Noun2Noun)
                .add(Noun2Adj.allConnections())
                .add(Noun2Adj);

        Noun_Su_Root.connections.add(Noun_Default.connections);
        Noun_Su_Root.indirectConnections.add(Noun_Default.indirectConnections).
                remove(P1sg_Im, P2sg_In, P3sg_sI, P1pl_ImIz, P2pl_InIz, Gen_nIn).add(
                P1sg_yIm, P2sg_yIn, P3sg_yI, P1pl_yImIz, P2pl_yInIz, Gen_yIn);

        ProperNoun_Template.connections.add(Noun_Default.connections);
        ProperNoun_Template.indirectConnections
                .add(Noun_Default.indirectConnections).remove(Related_sAl);

        ProperNoun_Default.copyConnections(ProperNoun_Template);

        Verb_TEMPLATE.connections.add(Neg_mA, Neg_m, Pos_EMPTY, Verb2Verb);
        Verb_TEMPLATE.indirectConnections
                .add(Prog_Iyor, Prog2_mAktA, Fut_yAcAk, Past_dI, Narr_mIs, Aor_Ir, Aor_Ar, Aor_z, AorPart_Ir_2Adj, AorPart_Ar_2Adj)
                .add(Abil_yAbil, Abil_yA, Caus_tIr, Caus_t, Opt_yA, Imp_TEMPLATE, Agt_yIcI_2Adj, Agt_yIcI_2Noun, Des_sA)
                .add(NotState_mAzlIk, ActOf_mAcA, PastPart_dIk_2Adj, PastPart_dIk_2Noun, NarrPart_mIs_2Adj, NarrPart_mIs_2Noun, Pass_In, Pass_nIl, Pass_InIl)
                .add(FutPart_yAcAk_2Adj, FutPart_yAcAk_2Noun, PresPart_yAn, AsLongAs_dIkcA, A2pl2_sAnIzA)
                .add(A1sg_yIm, A2sg_sIn, A2sg_TEMPLATE, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_yIn, A2pl_sInIz, A3pl_Verb_lAr, A3sg_sIn, A3pl_sInlAr, A2sg2_sAnA, A2sg3_yInIz)
                .add(Inf1_mAk, Inf2_mA, Inf3_yIs, Necess_mAlI)
                .add(Cond_sA, Cond_sA_AfterPerson)
                .add(When_yIncA, FeelLike_yAsI_2Adj, FeelLike_yAsI_2Noun, SinceDoing_yAlI, ByDoing_yArAk, WithoutDoing_mAdAn, WithoutDoing2_mAksIzIn)
                .add(AfterDoing_yIp, When_yIncA, UnableToDo_yAmAdAn, InsteadOfDoing_mAktAnsA, A3pl_Verb_lAr_After_Tense, AsIf_cAsInA)
                .add(KeepDoing2_yAdur, KeepDoing_yAgor, EverSince_yAgel, Almost_yAyAz, Hastily_yIver, Stay_yAkal, Start_yAkoy/*, Recip_Is*/)
                .add(UntilDoing_yAsIyA, Verb2VerbCompounds, Verb2Noun, Verb2Adv, Verb2Adj, Verb2NounPart, Verb2AdjPart, Verb2VerbAbility);

        Verb_Default.connections.add(Verb_TEMPLATE.connections);
        Verb_Default.indirectConnections.add(Verb_TEMPLATE.indirectConnections).remove(Caus_t);

/*        Verb_Reciprocal.connections.add(Verb_Default.connections);
        Verb_Reciprocal.indirectConnections.add(Verb_Default.indirectConnections).add(Recip_Is);*/

        Verb_Prog_Drop.connections.add(Pos_EMPTY);
        Verb_Prog_Drop.indirectConnections.add(Prog_Iyor).add(Prog_Iyor.allConnections());

        Verb_Ye.connections.add(Neg_m, Neg_mA, Pos_EMPTY, Verb2Verb);
        Verb_Ye.indirectConnections.add(Verb_Default.indirectConnections)
                .remove(Abil_yA, Abil_yAbil, Prog_Iyor, Fut_yAcAk, Caus_tIr,
                        FutPart_yAcAk_2Adj, Opt_yA, When_yIncA, AfterDoing_yIp, PresPart_yAn, KeepDoing_yAgor,
                        KeepDoing2_yAdur, UnableToDo_yAmAdAn).add(Pass_In/*, Recip_Is*/, Inf3_yIs);

        Verb_De_Ye_Prog.connections.add(Pos_EMPTY);
        Verb_De_Ye_Prog.indirectConnections.add(Prog_Iyor).add(Prog_Iyor.allConnections());

        Verb_Yi.connections.add(Pos_EMPTY, Verb2Verb);
        Verb_Yi.indirectConnections.add(Opt_yA, Fut_yAcAk, FutPart_yAcAk_2Adj, When_yIncA, AfterDoing_yIp, Abil_yA,
                Abil_yAbil/*, Recip_yIs*/, Inf3_yIs, FeelLike_yAsI_2Adj, PresPart_yAn, KeepDoing_yAgor, KeepDoing2_yAdur,
                FeelLike_yAsI_2Adj, UnableToDo_yAmAdAn, Verb2Adv, Verb2Adj, Verb2NounPart, Verb2AdjPart, Verb2VerbAbility);


        // modification rule does not apply for some suffixes for "demek". like deyip, not diyip

        Verb_De.connections.add(Neg_m, Neg_mA, Pos_EMPTY, Verb2Verb);
        Verb_De.indirectConnections.add(Verb_Default.indirectConnections)
                .remove(Abil_yA, Abil_yAbil, Prog_Iyor, Fut_yAcAk, FutPart_yAcAk_2Adj, Opt_yA,
                        PresPart_yAn, PresPart_yAn, KeepDoing_yAgor, KeepDoing2_yAdur, FeelLike_yAsI_2Adj, UnableToDo_yAmAdAn)
                .add(Pass_In);

        Verb_Di.connections.add(Pos_EMPTY, Verb2Verb);
        Verb_Di.indirectConnections.add(Opt_yA, Fut_yAcAk, FutPart_yAcAk_2Adj, Abil_yA, Abil_yAbil, PresPart_yAn,
                PresPart_yAn, KeepDoing_yAgor, KeepDoing2_yAdur, FeelLike_yAsI_2Adj, UnableToDo_yAmAdAn, Verb2Adv,
                Verb2Adj, Verb2NounPart, Verb2AdjPart, Verb2VerbAbility);

        //---------------------------- Noun -----------------------------------------------------------------------

        A3pl_lAr.connections.add(POSSESSIVE_FORMS).remove(P3pl_lArI).add(P3sg_yI, P3pl_I).remove(P3sg_sI);
        A3pl_lAr.indirectConnections
                .add(Noun2VerbCopular)
                .add(Noun2VerbCopular.allConnections()).remove(A3pl_Verb_lAr)
                .add(CASE_FORMS)
                .add(Dat_nA, Abl_ndAn, Loc_ndA, Acc_nI, Nom_TEMPLATE, Gen_nIn)
                .add(A1pl_yIz, A2pl_sInIz);

        //TODO: check below.
        A3pl_Comp_lAr.connections.add(A3pl_lAr.connections);
        A3pl_Comp_lAr.indirectConnections.add(CASE_FORMS)
                .add(A1pl_yIz, A2pl_sInIz);

        A3sg_TEMPLATE.connections.add(POSSESSIVE_FORMS).add(P1sg_yIm, P2sg_yIn, P3sg_yI, P1pl_yImIz, P2pl_yInIz);
        A3sg_TEMPLATE.indirectConnections
                .add(Noun_TEMPLATE.indirectConnections).remove(POSSESSIVE_FORMS).add(Gen_yIn)
                .add(Noun2Noun.allConnections()).add(Noun2Noun)
                .add(Noun2VerbCopular.allConnections())
                .add(Noun2Adj.allConnections().add(Noun2Adj));

        Nom_TEMPLATE.connections.add(Noun2Noun, Noun2Adj, Noun2Verb, Noun2VerbCopular);

        Nom_TEMPLATE.indirectConnections
                .add(Noun2Noun.allConnections())
                .add(Noun2Adj.allConnections())
                .add(Noun2VerbCopular.allConnections())
                .add(Noun2Verb.allConnections());

        Pres_TEMPLATE.connections.add(A1sg_yIm, A2sg_sIn, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr);
        Pres_TEMPLATE.indirectConnections.add(Cop_dIr);

        A1sg_yIm.connections.add(Cop_dIr);
        A2sg_sIn.connections.add(Cop_dIr);
        A3sg_Verb_TEMPLATE.connections.add(Cop_dIr, Verb2Adv);
        A3sg_Verb_TEMPLATE.indirectConnections.add(AsIf_cAsInA);
        A1pl_yIz.connections.add(Cop_dIr, Verb2Adv);
        A1pl_yIz.indirectConnections.add(AsIf_cAsInA);
        A2pl_sInIz.connections.add(Cop_dIr, Verb2Adv);
        A2pl_sInIz.indirectConnections.add(AsIf_cAsInA);
        A3pl_Verb_lAr.connections.add(Narr_mIs, Past_dI, Cond_sA, Cop_dIr, Verb2Adv);
        A3pl_Verb_lAr.indirectConnections.add(AsIf_cAsInA, While_ken);

        Dim_cIk.connections.add(Noun_Default.connections);
        Dim_cIk.indirectConnections.add(Noun_Default.allConnections().add(Noun2VerbCopular).remove(Dim_cIk, Dim2_cAgIz));

        Agt_cI.connections.add(Noun_Default.connections);
        Agt_cI.indirectConnections.add(Noun_Default.allConnections().add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections()).remove(Agt_cI));

        Dim2_cAgIz.connections.add(Noun_Default.connections);
        Dim2_cAgIz.indirectConnections.add(Noun_Default.allConnections().remove(Dim_cIk, Dim2_cAgIz));

        Ness_lIk.connections.add(Noun_Default.connections);
        Ness_lIk.indirectConnections.add(Noun_Default.allConnections().remove(Ness_lIk));

        Pnon_TEMPLATE.connections
                .add(CASE_FORMS)
                .add(Dat_nA, Loc_ndA, Abl_ndAn, Acc_nI, Gen_yIn);
        Pnon_TEMPLATE.indirectConnections
                .add(Nom_TEMPLATE.connections)
                .add(Noun2Noun.allConnections())
                .add(Noun2Verb.allConnections())
                .add(Noun2VerbCopular.allConnections())
                .add(Noun2Adj.allConnections());

        // TODO: check more thoroughly the Possesion-Verb-Person changes. suyusun, suyumuzsunuz etc..

        P1sg_Im.connections.add(CASE_FORMS);
        P1sg_Im.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections()).remove(A1pl_yIz, A1sg_yIm);

        // only for "su"
        P1sg_yIm.connections.add(CASE_FORMS);
        P1sg_yIm.indirectConnections.add(P1sg_Im.indirectConnections);

        P2sg_In.connections.add(CASE_FORMS);
        P2sg_In.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections()).remove(A2sg_sIn, A1pl_yIz, A2pl_nIz, A2pl_sInIz);

        // only for "su"
        P2sg_yIn.connections.add(CASE_FORMS);
        P2sg_yIn.indirectConnections.add(P2sg_In.indirectConnections);

        P3sg_sI.connections.add(Nom_TEMPLATE, Dat_nA, Loc_ndA, Abl_ndAn, Gen_nIn, Acc_nI, Inst_ylA, Equ_ncA);
        P3sg_sI.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections());

        P3sg_yI.connections.add(P3sg_sI.connections);
        P3sg_yI.indirectConnections.add(P3sg_sI.indirectConnections);

        P1pl_ImIz.connections.add(CASE_FORMS);
        P1pl_ImIz.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections()).remove(A1pl_yIz);

        P1pl_yImIz.connections.add(CASE_FORMS);
        P1pl_yImIz.indirectConnections.add(P1pl_ImIz.indirectConnections);

        P2pl_InIz.connections.add(CASE_FORMS);
        P2pl_InIz.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections());

        P2pl_yInIz.connections.add(CASE_FORMS);
        P2pl_yInIz.indirectConnections.add(P2pl_InIz.indirectConnections);

        P3pl_lArI.connections.add(Dat_nA, Abl_ndAn, Loc_ndA, Acc_nI, Nom_TEMPLATE, Gen_nIn);
        P3pl_lArI.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections());

        P3pl_I.connections.add(Dat_nA, Abl_ndAn, Loc_ndA, Acc_nI, Nom_TEMPLATE, Gen_nIn);
        P3pl_I.indirectConnections.add(Noun2VerbCopular).add(Noun2VerbCopular.allConnections());

        With_lI.connections.add(Adj_TEMPLATE.connections);
        With_lI.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        Related_sAl.connections.add(Adj_TEMPLATE.connections);
        Related_sAl.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        Without_sIz.connections.add(Adj_TEMPLATE.connections);
        Without_sIz.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        FitFor_lIk.connections.add(Adj_TEMPLATE.connections);
        FitFor_lIk.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        // Noun->Adj derivation elmadaki=elma+Loc-Adj+Rel
        Loc_dA.connections.add(Noun2Adj, Noun2VerbCopular);
        Loc_dA.indirectConnections.add(Rel_ki).add(Noun2VerbCopular.allConnections());

        Loc_ndA.connections.add(Noun2Adj, Noun2VerbCopular);
        Loc_ndA.indirectConnections.add(Rel_ki).add(Noun2VerbCopular.allConnections());

        Dat_nA.connections.add(Noun2VerbCopular);
        Dat_nA.indirectConnections.add(Noun2VerbCopular.allConnections());

        Dat_yA.connections.add(Noun2VerbCopular);
        Dat_yA.indirectConnections.add(Noun2VerbCopular.allConnections()).remove(A3pl_lAr, A3pl_Verb_lAr);

        Gen_nIn.connections.add(Noun2VerbCopular, Noun2Adj);
        Gen_nIn.indirectConnections.add(Noun2VerbCopular.allConnections()).add(Noun2Adj.connections);

        Abl_dAn.connections.add(Noun2VerbCopular);
        Abl_dAn.indirectConnections.add(Noun2VerbCopular.allConnections());

        Abl_ndAn.connections.add(Noun2VerbCopular);
        Abl_ndAn.indirectConnections.add(Noun2VerbCopular.allConnections());

        Inst_ylA.connections.add(Noun2VerbCopular);
        Inst_ylA.indirectConnections.add(Noun2VerbCopular.allConnections());

        Equ_cA.connections.add(Noun2VerbCopular);
        Equ_cA.indirectConnections.add(Noun2VerbCopular.allConnections());

        Rel_ki.connections.add(Adj2Noun);
        Rel_ki.indirectConnections.add(Adj2Noun.indirectConnections).add(A3sg_TEMPLATE, Pnon_TEMPLATE, Dat_nA, Loc_ndA, Abl_ndAn, Gen_nIn, Acc_nI, Inst_ylA, Equ_ncA);

        Rel_kI.connections.add(Adj2Noun);
        Rel_kI.indirectConnections.add(Adj2Noun.indirectConnections).add(A3sg_TEMPLATE, Pnon_TEMPLATE, Dat_nA, Loc_ndA, Abl_ndAn, Gen_nIn, Acc_nI, Inst_ylA, Equ_ncA);

        JustLike_msI.connections.add(Adj_TEMPLATE.connections);
        JustLike_msI.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        JustLike_ImsI.connections.add(Adj_TEMPLATE.connections);
        JustLike_ImsI.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        JustLike_Adj_msI.connections.add(Adj_TEMPLATE.connections);
        JustLike_Adj_msI.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        JustLike_Adj_ImsI.connections.add(Adj_TEMPLATE.connections);
        JustLike_Adj_ImsI.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        //---------------------------- Adjective -----------------------------------------------------------------------

        Become_lAs.connections.add(Verb_TEMPLATE.connections);
        Become_lAs.indirectConnections.add(Verb_TEMPLATE.indirectConnections).remove(Caus_t, Pass_In, Pass_InIl);

        Acquire_lAn.connections.add(Verb_TEMPLATE.connections);
        Acquire_lAn.indirectConnections.add(Verb_TEMPLATE.indirectConnections).remove(Caus_t, Pass_In, Pass_InIl);

        Become_Adj_lAs.connections.add(Verb_TEMPLATE.connections);
        Become_Adj_lAs.indirectConnections.add(Verb_TEMPLATE.indirectConnections).remove(Caus_t, Pass_In, Pass_InIl);

        Quite_cA.connections.add(Adj_TEMPLATE.connections);
        Quite_cA.indirectConnections.add(Adj_TEMPLATE.indirectConnections);

        Ly_cA.connections.add(Adv_TEMPLATE.connections);

        //---------------------------- Verb ----------------------------------------------------------------------------

        Pos_EMPTY.connections
                .add(Verb2VerbCompounds, Verb2Noun, Verb2Adv, Verb2Adj, Verb2AdjPart, Verb2NounPart, Verb2VerbAbility,
                        Imp_TEMPLATE, Prog_Iyor, Prog2_mAktA, Fut_yAcAk, Aor_Ar, Aor_Ir, Past_dI, Narr_mIs, ActOf_mAcA)
                .add(Cond_sA, Necess_mAlI, Opt_yA, Des_sA);
        Pos_EMPTY.indirectConnections
                .add(Verb_Default.indirectConnections)
                .add(A2pl2_sAnIzA, A2pl_yIn)
                .add(Verb2AdjPart.connections, Verb2NounPart.connections)
                .remove(Neg_m, Neg_mA);

        Neg_mA.connections.add(Verb2VerbCompounds, Verb2VerbAbility, Verb2Noun, Verb2Adv, Verb2Adj, Verb2AdjPart, Verb2NounPart,
                Aor_z, Aor_EMPTY, Prog2_mAktA, Imp_TEMPLATE, Opt_yA, Des_sA,
                Fut_yAcAk, Past_dI, Narr_mIs, Necess_mAlI, NotState_mAzlIk,
                ActOf_mAcA);

        Neg_mA.indirectConnections.add(Verb2VerbCompounds.connections, Verb2AdjPart.connections, Verb2NounPart.connections,
                Verb2Noun.connections, Verb2Adv.connections, Verb2Adj.connections)
                .add(A2sg_TEMPLATE, A1sg_m, A1sg_yIm, A2sg_sIn, A2sg2_sAnA, A2sg3_yInIz, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, A2pl2_sAnIzA, A2pl_yIn, A3pl_Verb_lAr, A3sg_sIn, A3pl_sInlAr)
                .add(Abil_yAbil);

        Neg_m.connections.add(Prog_Iyor);

        Imp_TEMPLATE.connections.add(A2sg_TEMPLATE, A2sg2_sAnA, A2sg3_yInIz, A2pl2_sAnIzA, A2pl_yIn, A3sg_sIn, A3pl_sInlAr);

        Caus_t.connections.add(Verb2Verb, Pos_EMPTY, Neg_mA, Neg_m);
        Caus_t.indirectConnections.add(Verb_TEMPLATE.indirectConnections).add(Pass_nIl).add(Caus_tIr).remove(Caus_t);

        Caus_tIr.connections.add(Verb_TEMPLATE.connections);
        Caus_tIr.indirectConnections.add(Verb_TEMPLATE.indirectConnections).add(Pass_nIl).add(Caus_t).remove(Caus_tIr);

/*
        Recip_Is.connections.add(Verb_TEMPLATE.connections);
        Recip_Is.indirectConnections.add(Verb_TEMPLATE.indirectConnections).remove(Caus_t, Pass_nIl, Recip_Is, Recip_yIs);
        Recip_yIs.connections.add(Verb_TEMPLATE.connections);
        Recip_yIs.indirectConnections.add(Verb_TEMPLATE.indirectConnections).remove(Caus_t, Caus_tIr, Pass_nIl, Pass_InIl, Pass_In, Recip_Is, Recip_yIs);
*/

        Pass_nIl.connections.add(Verb_TEMPLATE.connections);
        Pass_nIl.indirectConnections.add(Verb_TEMPLATE.indirectConnections)
                .remove(Caus_t, Caus_tIr, Pass_nIl, Pass_InIl, Pass_In);

        Pass_In.connections.add(Verb_TEMPLATE.connections);
        Pass_In.indirectConnections.add(Verb_TEMPLATE.indirectConnections)
                .remove(Caus_t, Caus_tIr, Pass_nIl, Pass_InIl, Pass_In);

        Pass_InIl.connections.add(Verb_TEMPLATE.connections);
        Pass_InIl.indirectConnections.add(Verb_TEMPLATE.indirectConnections)
                .remove(Caus_t, Caus_tIr, Pass_nIl, Pass_InIl, Pass_In);

        Prog_Iyor.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);

        Prog2_mAktA.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);

        Fut_yAcAk.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);
        Fut_yAcAk.indirectConnections.add(A3sg_Verb_TEMPLATE.allConnections());


        Aor_Ar.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);
        Aor_Ar.indirectConnections.add(Verb2Adv, AsIf_cAsInA);
        Aor_Ir.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);
        Aor_Ir.indirectConnections.add(Verb2Adv, AsIf_cAsInA);
        Aor_z.connections.add(A3sg_Verb_TEMPLATE, A3sg_sIn, A2pl_sInIz, A3pl_Verb_lAr).add(PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken, Cop_dIr);
        Aor_z.indirectConnections.add(Verb2Adv, AsIf_cAsInA);
        Aor_EMPTY.connections.add(A1sg_m, A1pl_yIz);

        // TODO: Participle suffixes should allow less successors.
        AorPart_Ar_2Adj.connections.add(Adj2Noun);
        AorPart_Ar_2Adj.indirectConnections.add(Adj2Noun.allConnections());
        AorPart_Ir_2Adj.connections.add(Adj2Noun);
        AorPart_Ir_2Adj.indirectConnections.add(Adj2Noun.allConnections());
        AorPart_z_2Adj.connections.add(Adj2Noun);
        AorPart_z_2Adj.indirectConnections.add(Adj2Noun.allConnections());

        PastPart_dIk_2Noun.connections.add(A3sg_TEMPLATE).add(A3pl_lAr);
        PastPart_dIk_2Noun.indirectConnections.add(POSSESSIVE_FORMS, CASE_FORMS).remove(Equ_cA);

        NarrPart_mIs_2Noun.connections.add(A3pl_lAr, A3sg_TEMPLATE);
        NarrPart_mIs_2Noun.indirectConnections.add(POSSESSIVE_FORMS, CASE_FORMS);

        // Oflazer suggests only with A3pl. I think A3sg is also possible.
        FutPart_yAcAk_2Noun.connections.add(A3pl_lAr, A3sg_TEMPLATE);
        // TODO: for allowing "yapacağa". But removing Noun2Verb does not work such as "yapacağaymış" false positive.
        FutPart_yAcAk_2Noun.indirectConnections.add(POSSESSIVE_FORMS).add(Dat_yA)
                .remove(Noun2Verb, Noun2VerbCopular);

        PastPart_dIk_2Adj.connections.add(POSSESSIVE_FORMS);
        PastPart_dIk_2Adj.indirectConnections.add(POSSESSIVE_FORMS).remove(CASE_FORMS);


        NarrPart_mIs_2Adj.connections.add(Adj2Noun);
        NarrPart_mIs_2Adj.indirectConnections.add(Ness_lIk).add(Ness_lIk.allConnections());


/*        FutPart_yAcAk_2Adj.connections.add(Adj2Noun);
        FutPart_yAcAk_2Adj.indirectConnections.add(Adj2Noun.allConnections());*/

        PresPart_yAn.connections.add(Adj2Noun);
        PresPart_yAn.indirectConnections.add(Adj2Noun.allConnections());

        Past_dI.connections.add(A1sg_m, A2sg_n, A3sg_Verb_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_Verb_lAr, CondCop_ysA, PastCop_ydI);
        A1sg_m.connections.add(Cond_sA_AfterPerson);
        A2sg_n.connections.add(Cond_sA_AfterPerson);
        A1pl_k.connections.add(Cond_sA_AfterPerson);
        A2pl_nIz.connections.add(Cond_sA_AfterPerson);
        A3pl_Verb_lAr.connections.add(Cond_sA_AfterPerson);

        Narr_mIs.connections.add(A1sg_yIm, A2sg_sIn, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr)
                .add(CondCop_ysA, PastCop_ydI, NarrCop_ymIs, While_ken, Cop_dIr);
        Narr_mIs.indirectConnections.add(A3sg_Verb_TEMPLATE.allConnections());

        Cond_sA.connections.add(A1sg_m, A2sg_n, A3sg_Verb_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_Verb_lAr, PastCop_ydI, NarrCop_ymIs);

        PastCop_ydI.connections.add(PERSON_FORMS_COP);
        NarrCop_ymIs.connections.add(A1sg_yIm, A2sg_sIn, A3sg_Verb_TEMPLATE, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr);
        NarrCop_ymIs.indirectConnections.add(A3sg_Verb_TEMPLATE.allConnections());
        CondCop_ysA.connections.add(PERSON_FORMS_COP);
        Cop_dIr.connections.add(A3pl_Verb_lAr);

        // TODO: may be too broad
        Inf1_mAk.connections.add(A3sg_TEMPLATE);
        Inf1_mAk.indirectConnections.add(Pnon_TEMPLATE, Nom_TEMPLATE, Inst_ylA, Loc_dA, Dat_yA, Abl_dAn, Acc_yI, Noun2Adj, Noun2Noun, Noun2VerbCopular);
        Inf1_mAk.indirectConnections.add(Noun2Adj.allConnections(), Noun2Noun.allConnections(), Noun2VerbCopular.allConnections());
        Inf1_mAk.indirectConnections.remove(Without_sIz); // gelmeksizin only parses with WithoutDoing2(-sizin)
        Inf2_mA.connections.add(Noun_Default.connections);
        Inf2_mA.indirectConnections.add(Noun_Default.indirectConnections);
        Inf3_yIs.connections.add(Noun_Default.connections);
        Inf3_yIs.indirectConnections.add(Noun_Default.indirectConnections);
        ActOf_mAcA.connections.add(Noun_Default.connections);
        ActOf_mAcA.indirectConnections.add(Noun_Default.indirectConnections);
        NotState_mAzlIk.connections.add(Noun_Default.connections);
        NotState_mAzlIk.indirectConnections.add(Noun_Default.indirectConnections);

        Abil_yA.connections.add(Neg_mA, Neg_m);
        Abil_yA.indirectConnections.add(Neg_mA.allConnections());

        Abil_yAbil.connections.add(Neg_mA.connections).add(Aor_Ir, Prog_Iyor, AorPart_Ir_2Adj)
                .remove(Verb2VerbAbility, Aor_Ar, AorPart_Ar_2Adj);
        Abil_yAbil.indirectConnections.add(Neg_mA.indirectConnections).remove(Aor_Ar, AorPart_Ar_2Adj);

        // TODO: removing false positives like geliveriver may be necessary

        KeepDoing_yAgor.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        KeepDoing_yAgor.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        KeepDoing2_yAdur.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        KeepDoing2_yAdur.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        EverSince_yAgel.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        EverSince_yAgel.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        Almost_yAyAz.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        Almost_yAyAz.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        Hastily_yIver.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        Hastily_yIver.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        Stay_yAkal.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        Stay_yAkal.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        Start_yAkoy.connections.add(Pos_EMPTY.connections, Neg_mA.connections);
        Start_yAkoy.indirectConnections.add(Pos_EMPTY.indirectConnections, Neg_mA.indirectConnections);

        Necess_mAlI.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_yIz, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);

        Des_sA.connections.add(A1sg_m, A2sg_n, A3sg_TEMPLATE, A1pl_k, A2pl_nIz, A3pl_lAr, PastCop_ydI, NarrCop_ymIs);

        When_yIncA.connections.add(Adv2Noun);
        When_yIncA.indirectConnections.add(Adv2Noun.allConnections());

        //TODO ararkendi ararkenmiş legal?
        While_ken.connections.add(Adv2Adj);
        While_ken.indirectConnections.add(Rel_ki);

        // TODO: FeelLike_yAsI_2Noun, Agt_yIcI_Noun may be too broad.
        FeelLike_yAsI_2Noun.connections.add(Noun_Default.connections);
        FeelLike_yAsI_2Noun.indirectConnections.add(Noun_Default.indirectConnections);

        FeelLike_yAsI_2Adj.connections.add(Adj_TEMPLATE);

        Agt_yIcI_2Noun.connections.add(Noun_Default.connections);
        Agt_yIcI_2Noun.indirectConnections.add(Noun_Default.indirectConnections);

        Agt_yIcI_2Adj.connections.add(Adj_TEMPLATE);

        Opt_yA.connections.add(A3sg_Verb_TEMPLATE, A1sg_yIm, A2sg_sIn, A1pl_lIm, A2pl_sInIz, A3pl_Verb_lAr).add(COPULAR_FORMS);

        A1sg_TEMPLATE.connections.add(Pnon_TEMPLATE).remove(POSSESSIVE_FORMS).add(P1sg_Im);
        A1sg_TEMPLATE.indirectConnections.add(CASE_FORMS).add(Pnon_TEMPLATE).add(Noun2Verb, Noun2VerbCopular);

        A2pl_TEMPLATE.connections.add(Pnon_TEMPLATE);
        A2pl_TEMPLATE.indirectConnections.add(Pnon_TEMPLATE.allConnections());
        A2pl_ler.connections.add(Pnon_TEMPLATE);
        A2pl_ler.indirectConnections.add(Pnon_TEMPLATE.allConnections().remove(A3pl_Verb_lAr, A3pl_lAr, A3pl_sInlAr));

        A1pl_TEMPLATE.connections.add(Pnon_TEMPLATE, P1pl_ImIz);
        A1pl_TEMPLATE.indirectConnections.add(Pnon_TEMPLATE.allConnections());
        A1pl_ler.connections.add(Pnon_TEMPLATE);
        A1pl_ler.indirectConnections.add(Pnon_TEMPLATE.allConnections().remove(A3pl_Verb_lAr, A3pl_lAr, A3pl_sInlAr));

        //PersPron_TEMPLATE.connections.add()
        PersPron_TEMPLATE.connections.add(A1sg_TEMPLATE, A2sg_TEMPLATE, A3sg_TEMPLATE, A1pl_TEMPLATE, A2pl_TEMPLATE);
        PersPron_TEMPLATE.indirectConnections.add(CASE_FORMS).add(Pnon_TEMPLATE).add(Noun2Verb, Noun2VerbCopular);

        PersPron_Ben.connections.add(A1sg_TEMPLATE);
        PersPron_Ben.indirectConnections.add(PersPron_TEMPLATE.indirectConnections);

        PersPron_BanSan.connections.add(A1sg_TEMPLATE);
        PersPron_BanSan.indirectConnections.add(PersPron_TEMPLATE.indirectConnections).remove(CASE_FORMS).add(Dat_yA);

        PersPron_Sen.connections.add(A2sg_TEMPLATE);
        PersPron_Sen.indirectConnections.add(PersPron_TEMPLATE.indirectConnections);

        PersPron_O.connections.add(A3sg_TEMPLATE);
        PersPron_O.indirectConnections.add(PersPron_TEMPLATE.indirectConnections);

        PersPron_Biz.connections.add(A1pl_TEMPLATE, A1pl_ler);
        PersPron_Biz.indirectConnections.add(A1pl_ler.allConnections());

        PersPron_Siz.connections.add(A2pl_TEMPLATE, A2pl_ler);
        PersPron_Siz.indirectConnections.add(A2pl_ler.allConnections());
        registerForms(
                Noun_TEMPLATE, Verb_TEMPLATE, Adj_TEMPLATE, Adv_TEMPLATE, Numeral_Template, Postp_Template, Dup_Template,
                PersPron_TEMPLATE, DemonsPron_TEMPLATE, ReflexPron_TEMPLATE, Det_Template, QuantPron_TEMPLATE,
                Conj_Template, Ques_Template, QuesPron_TEMPLATE, Punc_Template,

                Noun2Adj, Noun2Noun, Noun2Verb, Noun2VerbCopular,
                Adj2Adj, Adj2Adv, Adj2Noun, Adj2Verb,
                Verb2Adj, Verb2Verb, Verb2VerbCompounds, Verb2Noun, Verb2Adv,
                Postp2Noun, Pron2Verb,

               /* Verb_Reciprocal,*/

                Pres_TEMPLATE,

                Noun_Default, Noun_Time_Default, Det_Default,
                ProperNoun_Default,
                Verb_Default, Adj_Default, Numeral_Default, Conj_Default,
                PersPron_Default, QuantPron_Default, DemonsPron_Default, ReflexPron_Default,
                Postp_Default, Dup_Default, Ques_Template, QuesPron_TEMPLATE, Punc_Default,

                JustLike_Adj_ImsI, JustLike_msI,
                Noun_Su_Root,
                Pass_InIl,
                Nom_TEMPLATE, Dat_yA, Dat_nA, Loc_dA, Loc_ndA, Abl_dAn, Abl_ndAn, Gen_nIn,
                Acc_yI, Acc_nI, Inst_ylA,
                Pnon_TEMPLATE, P1sg_Im, P2sg_In, P3sg_sI, P1pl_ImIz, P2pl_InIz, P3pl_lArI, P3pl_I,
                P1sg_yIm, P2sg_yIn, P3sg_yI, P1pl_yImIz, P2pl_yInIz, Gen_yIn,// su-*
                Dim_cIk, Dim2_cAgIz,
                With_lI, Without_sIz, Rel_ki, Rel_kI,

                A1sg_yIm, A1sg_m, A1sg_TEMPLATE, A2sg_sIn, A2sg_n, A2sg_TEMPLATE, A2sg2_sAnA,
                A3sg_TEMPLATE, A3sg_Verb_TEMPLATE, A2sg3_yInIz, A3sg_sIn,
                A1pl_yIz, A1pl_k, A1pl_lIm, A1pl_TEMPLATE, A1pl_ler,
                A2pl_sInIz, A2pl_nIz, A2pl_yIn, A2pl_TEMPLATE, A2pl2_sAnIzA, A2pl_ler,
                A3pl_lAr, A3pl_Verb_lAr, A3pl_sInlAr, A3pl_nlAr,
                Agt_cI, Agt_yIcI_2Adj, Agt_yIcI_2Noun,

                Num2Noun, Num2Adj, Num2Adv, Num2Verb,

                PersPron_Siz, PersPron_BanSan, PersPron_Biz, PersPron_O, PersPron_Sen, PersPron_Ben,

                Ness_lIk, FitFor_lIk,
                Become_lAs, Become_Adj_lAs, Acquire_lAn,
                JustLike_ImsI, JustLike_msI, Related_sAl,
                Aor_Ir, Aor_Ar, Aor_z, Des_sA,
                Aor_EMPTY, AorPart_Ar_2Adj, AorPart_Ir_2Adj, AorPart_z_2Adj,
                Prog_Iyor, Prog2_mAktA, Fut_yAcAk,
                FutPart_yAcAk_2Adj, FutPart_yAcAk_2Noun, Past_dI, PastPart_dIk_2Noun, PastPart_dIk_2Adj,
                Narr_mIs, NarrPart_mIs_2Adj, NarrPart_mIs_2Noun, PresPart_yAn, Neg_mA, Neg_m,
                Cond_sA, Cond_sA_AfterPerson,
                Necess_mAlI, Opt_yA,
                Pass_In, Pass_nIl,
                Caus_t, Caus_tIr,
                Imp_TEMPLATE, /*Recip_Is, Recip_yIs,*/ Reflex_In, Abil_yAbil, Abil_yA, Cop_dIr,
                PastCop_ydI, NarrCop_ymIs, CondCop_ysA, While_ken, NotState_mAzlIk, ActOf_mAcA,
                AsIf_cAsInA, AsLongAs_dIkcA, When_yIncA, FeelLike_yAsI_2Adj, FeelLike_yAsI_2Noun, SinceDoing_yAlI,
                ByDoing_yArAk, WithoutDoing_mAdAn,
                WithoutDoing2_mAksIzIn, AfterDoing_yIp, UnableToDo_yAmAdAn, InsteadOfDoing_mAktAnsA,
                KeepDoing_yAgor, KeepDoing2_yAdur, EverSince_yAgel, Start_yAkoy,
                Almost_yAyAz, Hastily_yIver, Stay_yAkal, Inf1_mAk, Inf2_mA, Inf3_yIs, Ly_cA,
                Quite_cA, Equ_cA, Equ_ncA, UntilDoing_yAsIyA,
                A3pl_Comp_lAr, Interj_Template, Verb_Prog_Drop,
                Ordinal_IncI, Grouping_sAr);
    }

    @Override
    public SuffixForm getRootSet(DictionaryItem item, SuffixData suffixConstraint) {
        if (item.specialRootSuffix != null) {
            return item.specialRootSuffix;
        }
        if (suffixConstraint.isEmpty()) {
            switch (item.primaryPos) {
                case Noun:
                    if (item.hasAttribute(RootAttribute.CompoundP3sg))
                        return Noun_Comp_P3sg;
                    if (item.hasAttribute(RootAttribute.CompoundP3sgRoot))
                        return Noun_Comp_P3sg_Root;
                    switch (item.secondaryPos) {
                        case ProperNoun:
                            return ProperNoun_Default;
                        case Time:
                            return Noun_Time_Default;
                        default:
                            return Noun_Default;
                    }
                case Adjective:
                    return Adj_Default;
                case Verb:
                    return Verb_Default;
                case Adverb:
                    return Adv_Default;
                case Numeral:
                    return Numeral_Default;
                case Interjection:
                    return Interj_Default;
                case Question:
                    return Ques_Default;
                case Conjunction:
                    return Conj_Default;
                case PostPositive:
                    return Postp_Default;
                case Punctuation:
                    return Punc_Default;
                case Determiner:
                    return Det_Default;
                case Duplicator:
                    return Dup_Default;
                case Pronoun:
                    switch (item.secondaryPos) {
                        case Demonstrative:
                            return DemonsPron_Default;
                        case Quantitive:
                            return QuantPron_Default;
                        case Question:
                            return QuesPron_Default;
                        case Reflexive:
                            return ReflexPron_Default;
                        default:
                            return PersPron_Default;
                    }
                default:
                    return Noun_Default;
            }
        } else {
            SuffixFormTemplate template;
            switch (item.primaryPos) {
                case Noun:
                    template = Noun_TEMPLATE;
                    break;
                case Adjective:
                    template = Adj_TEMPLATE;
                    break;
                case Verb:
                    template = Verb_TEMPLATE;
                    break;
                case PostPositive:
                    template = Postp_Template;
                    break;
                case Pronoun:
                    if (item.secondaryPos == SecondaryPos.Demonstrative)
                        template = DemonsPron_TEMPLATE;
                    else if (item.secondaryPos == SecondaryPos.Quantitive)
                        template = QuantPron_TEMPLATE;
                    else if (item.secondaryPos == SecondaryPos.Question)
                        template = QuesPron_TEMPLATE;
                    else
                        return PersPron_TEMPLATE;
                    break;
                default:
                    template = Noun_TEMPLATE;

            }
            NullSuffixForm copy = generateNullFormFromTemplate(template, suffixConstraint).copy();
            registerForm(copy);
            return copy;
        }
    }

    @Override
    public SuffixData[] defineSuccessorSuffixes(DictionaryItem item) {
        SuffixData original = new SuffixData();
        SuffixData modified = new SuffixData();

        PrimaryPos primaryPos = item.primaryPos;

        switch (primaryPos) {
            case Verb:
                getForVerb(item, original, modified);
                break;
            default:
                break;
        }
        return new SuffixData[]{original, modified};
    }

    private void getForVerb(DictionaryItem item, SuffixData original, SuffixData modified) {
        original.add(Verb_TEMPLATE.allConnections().remove(Caus_t));
        modified.add(Verb_TEMPLATE.allConnections().remove(Caus_t));
        for (RootAttribute attribute : item.attributes) {
            switch (attribute) {
                case Aorist_A:
                    original.add(Aor_Ar, AorPart_Ar_2Adj);
                    original.remove(Aor_Ir, AorPart_Ir_2Adj);
                    if (!item.attributes.contains(RootAttribute.ProgressiveVowelDrop)) {
                        modified.add(Aor_Ar, AorPart_Ar_2Adj);
                        modified.remove(Aor_Ir, AorPart_Ir_2Adj);
                    }
                    break;
                case Aorist_I:
                    original.add(Aor_Ir, AorPart_Ir_2Adj);
                    original.remove(Aor_Ar, AorPart_Ar_2Adj);
                    if (!item.attributes.contains(RootAttribute.ProgressiveVowelDrop)) {
                        modified.add(Aor_Ir, AorPart_Ir_2Adj);
                        modified.remove(Aor_Ar, AorPart_Ar_2Adj);
                    }
                    break;
                case Voicing:
                    modified.remove(Pass_In);
                    modified.remove(Pass_InIl);
                    break;

                case Passive_In:
                    original.add(Pass_In);
                    original.add(Pass_InIl);
                    original.remove(Pass_nIl);
                    break;
                case LastVowelDrop:
                    original.remove(Pass_nIl);
                    modified.clear().add(Pass_nIl, Verb2Verb);
                    break;
                case ProgressiveVowelDrop:
                    original.remove(Prog_Iyor);
                    modified.clear().add(Pos_EMPTY, Prog_Iyor);
                    break;
                case NonTransitive:
                    original.remove(Caus_t, Caus_tIr);
                    modified.remove(Caus_t, Caus_tIr);
                    break;
                case Reflexive:
                    original.add(Reflex_In);
                    modified.add(Reflex_In);
                    break;
/*
                case Reciprocal:
                    original.add(Recip_Is);
                    modified.add(Recip_Is);
                    break;
*/
                case Causative_t:
                    original.remove(Caus_tIr);
                    original.add(Caus_t);
                    if (!item.attributes.contains(RootAttribute.ProgressiveVowelDrop)) {
                        modified.remove(Caus_tIr);
                        modified.add(Caus_t);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}