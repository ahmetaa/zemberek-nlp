package zemberek.morphology.morphotactics;

import static zemberek.morphology.morphotactics.MorphemeState.builder;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminal;
import static zemberek.morphology.morphotactics.MorphemeState.nonTerminalDerivative;
import static zemberek.morphology.morphotactics.MorphemeState.terminal;
import static zemberek.morphology.morphotactics.TurkishMorphotactics.*;

/**
 * Shared registry of all {@link MorphemeState} nodes in a {@link TurkishMorphotactics} graph.
 * Keeping state declarations separate from transition wiring avoids circular dependencies across
 * cross-POS derivations (e.g., Verb -> Noun/Adj, Noun -> Verb/Pron).
 */
final class MorphotacticStates {

  // _ST = Terminal state, _S = Non-terminal state.

  //-------------- Root & Punctuation ------------------------

  final MorphemeState root_S = nonTerminal("root_S", root);
  final MorphemeState puncRoot_ST = builder("puncRoot_ST", punc).terminal().posRoot().build();

  //-------------- Noun States ------------------------

  final MorphemeState noun_S = builder("noun_S", noun).posRoot().build();
  final MorphemeState nounCompoundRoot_S = builder("nounCompoundRoot_S", noun).posRoot().build();
  final MorphemeState nounSuRoot_S = builder("nounSuRoot_S", noun).posRoot().build();
  final MorphemeState nounInf1Root_S = builder("nounInf1Root_S", noun).posRoot().build();
  final MorphemeState nounActOfRoot_S = builder("nounActOfRoot_S", noun).posRoot().build();

  final MorphemeState a3sg_S = nonTerminal("a3sg_S", a3sg);
  final MorphemeState a3sgSu_S = nonTerminal("a3sgSu_S", a3sg);
  final MorphemeState a3sgCompound_S = nonTerminal("a3sgCompound_S", a3sg);
  final MorphemeState a3sgInf1_S = nonTerminal("a3sgInf1_S", a3sg);
  final MorphemeState a3sgActOf_S = nonTerminal("a3sgActOf_S", a3sg);
  final MorphemeState a3pl_S = nonTerminal("a3pl_S", a3pl);
  final MorphemeState a3plActOf_S = nonTerminal("a3plActOf_S", a3pl);
  final MorphemeState a3plCompound_S = nonTerminal("a3plCompound_S", a3pl);
  final MorphemeState a3plCompound2_S = nonTerminal("a3plCompound2_S", a3pl);

  final MorphemeState pnon_S = nonTerminal("pnon_S", pnon);
  final MorphemeState pnonCompound_S = nonTerminal("pnonCompound_S", pnon);
  final MorphemeState pnonCompound2_S = nonTerminal("pnonCompound2_S", pnon);
  final MorphemeState pnonInf1_S = nonTerminal("pnonInf1_S", pnon);
  final MorphemeState pnonActOf = nonTerminal("pnonActOf", pnon);
  final MorphemeState p1sg_S = nonTerminal("p1sg_S", p1sg);
  final MorphemeState p2sg_S = nonTerminal("p2sg_S", p2sg);
  final MorphemeState p3sg_S = nonTerminal("p3sg_S", p3sg);
  final MorphemeState p1pl_S = nonTerminal("p1pl_S", p1pl);
  final MorphemeState p2pl_S = nonTerminal("p2pl_S", p2pl);
  final MorphemeState p3pl_S = nonTerminal("p3pl_S", p3pl);

  final MorphemeState nom_ST = terminal("nom_ST", nom);
  final MorphemeState nom_S = nonTerminal("nom_S", nom);
  final MorphemeState dat_ST = terminal("dat_ST", dat);
  final MorphemeState abl_ST = terminal("abl_ST", abl);
  final MorphemeState loc_ST = terminal("loc_ST", loc);
  final MorphemeState ins_ST = terminal("ins_ST", ins);
  final MorphemeState acc_ST = terminal("acc_ST", acc);
  final MorphemeState gen_ST = terminal("gen_ST", gen);
  final MorphemeState equ_ST = terminal("equ_ST", equ);

  final MorphemeState dim_S = nonTerminalDerivative("dim_S", dim);
  final MorphemeState ness_S = nonTerminalDerivative("ness_S", ness);
  final MorphemeState agt_S = nonTerminalDerivative("agt_S", agt);
  final MorphemeState related_S = nonTerminalDerivative("related_S", related);
  final MorphemeState rel_S = nonTerminalDerivative("rel_S", rel);
  final MorphemeState relToPron_S = nonTerminalDerivative("relToPron_S", rel);
  final MorphemeState with_S = nonTerminalDerivative("with_S", with);
  final MorphemeState without_S = nonTerminalDerivative("without_S", without);
  final MorphemeState justLike_S = nonTerminalDerivative("justLike_S", justLike);
  final MorphemeState nounZeroDeriv_S = nonTerminalDerivative("nounZeroDeriv_S", zero);
  final MorphemeState become_S = nonTerminalDerivative("become_S", become);
  final MorphemeState acquire_S = nonTerminalDerivative("acquire_S", acquire);

  //-------------- Last Vowel Drop & Proper/Abbrv Nouns ------------------------

  final MorphemeState nounLastVowelDropRoot_S =
      builder("nounLastVowelDropRoot_S", noun).posRoot().build();
  final MorphemeState adjLastVowelDropRoot_S =
      builder("adjLastVowelDropRoot_S", adj).posRoot().build();
  final MorphemeState postpLastVowelDropRoot_S =
      builder("postpLastVowelDropRoot_S", postp).posRoot().build();
  final MorphemeState a3PlLastVowelDrop_S = nonTerminal("a3PlLastVowelDrop_S", a3pl);
  final MorphemeState a3sgLastVowelDrop_S = nonTerminal("a3sgLastVowelDrop_S", a3sg);
  final MorphemeState pNonLastVowelDrop_S = nonTerminal("pNonLastVowelDrop_S", pnon);
  final MorphemeState zeroLastVowelDrop_S = nonTerminalDerivative("zeroLastVowelDrop_S", zero);

  final MorphemeState nounProper_S = builder("nounProper_S", noun).posRoot().build();
  final MorphemeState nounAbbrv_S = builder("nounAbbrv_S", noun).posRoot().build();
  final MorphemeState puncProperSeparator_S = nonTerminal("puncProperSeparator_S", punc);
  final MorphemeState nounNoSuffix_S = builder("nounNoSuffix_S", noun).posRoot().build();
  final MorphemeState nounA3sgNoSuffix_S = nonTerminal("nounA3sgNoSuffix_S", a3sg);
  final MorphemeState nounPnonNoSuffix_S = nonTerminal("nounPnonNoSuffix_S", pnon);
  final MorphemeState nounNomNoSuffix_ST = terminal("nounNomNoSuffix_S", nom);

  //-------------- Adjective & Numeral States ------------------------

  final MorphemeState adjectiveRoot_ST =
      builder("adjectiveRoot_ST", adj).terminal().posRoot().build();
  final MorphemeState adjAfterVerb_S = builder("adjAfterVerb_S", adj).posRoot().build();
  final MorphemeState adjAfterVerb_ST =
      builder("adjAfterVerb_ST", adj).terminal().posRoot().build();
  final MorphemeState adjZeroDeriv_S = nonTerminalDerivative("adjZeroDeriv_S", zero);

  final MorphemeState aPnon_ST = terminal("aPnon_ST", pnon);
  final MorphemeState aP1sg_ST = terminal("aP1sg_ST", p1sg);
  final MorphemeState aP2sg_ST = terminal("aP2sg_ST", p2sg);
  final MorphemeState aP3sg_ST = terminal("aP3sg_ST", p3sg);
  final MorphemeState aP1pl_ST = terminal("aP1pl_ST", p1pl);
  final MorphemeState aP2pl_ST = terminal("aP2pl_ST", p2pl);
  final MorphemeState aP3pl_ST = terminal("aP3pl_ST", p3pl);

  final MorphemeState aLy_S = nonTerminalDerivative("aLy_S", ly);
  final MorphemeState aAsIf_S = nonTerminalDerivative("aAsIf_S", asIf);
  final MorphemeState aAgt_S = nonTerminalDerivative("aAgt_S", agt);

  final MorphemeState aorPartNegZero_S = nonTerminalDerivative("aorPartNegZero_S", zero);
  final MorphemeState aorPartNegNoun_S = builder("aorPartNegNoun_S", noun).posRoot().build();
  final MorphemeState aorPartNegA3sg_S = nonTerminal("aorPartNegA3sg_S", a3sg);
  final MorphemeState aorPartNegPnon_S = nonTerminal("aorPartNegPnon_S", pnon);
  final MorphemeState aorPartNegAbl_ST = terminal("aorPartNegAbl_ST", abl);

  final MorphemeState numeralRoot_ST = builder("numeralRoot_ST", num).terminal().posRoot().build();
  final MorphemeState numZeroDeriv_S = nonTerminalDerivative("numZeroDeriv_S", zero);

  //-------------- Nominal/Adjective Predicate (nVerb) States ------------------------

  final MorphemeState nVerb_S = builder("nVerb_S", verb).posRoot().build();
  final MorphemeState nVerbDegil_S = builder("nVerbDegil_S", verb).posRoot().build();

  final MorphemeState nPresent_S = nonTerminal("nPresent_S", pres);
  final MorphemeState nPast_S = nonTerminal("nPast_S", past);
  final MorphemeState nNarr_S = nonTerminal("nNarr_S", narr);
  final MorphemeState nCond_S = nonTerminal("nCond_S", cond);
  final MorphemeState nA1sg_ST = terminal("nA1sg_ST", a1sg);
  final MorphemeState nA2sg_ST = terminal("nA2sg_ST", a2sg);
  final MorphemeState nA1pl_ST = terminal("nA1pl_ST", a1pl);
  final MorphemeState nA2pl_ST = terminal("nA2pl_ST", a2pl);
  final MorphemeState nA3sg_ST = terminal("nA3sg_ST", a3sg);
  final MorphemeState nA3sg_S = nonTerminal("nA3sg_S", a3sg);
  final MorphemeState nA3pl_ST = terminal("nA3pl_ST", a3pl);

  final MorphemeState nCop_ST = terminal("nCop_ST", cop);
  final MorphemeState nCopBeforeA3pl_S = nonTerminal("nCopBeforeA3pl_S", cop);
  final MorphemeState nNeg_S = nonTerminal("nNeg_S", neg);

  //-------------- Pronoun & Pronoun Predicate States ------------------------

  final MorphemeState pronPers_S = builder("pronPers_S", pron).posRoot().build();
  final MorphemeState pronDemons_S = builder("pronDemons_S", pron).posRoot().build();
  final MorphemeState pronQuant_S = builder("pronQuant_S", pron).posRoot().build();
  final MorphemeState pronQuantModified_S = builder("pronQuantModified_S", pron).posRoot().build();
  final MorphemeState pronQues_S = builder("pronQues_S", pron).posRoot().build();
  final MorphemeState pronReflex_S = builder("pronReflex_S", pron).posRoot().build();
  final MorphemeState pronPers_Mod_S = builder("pronPers_Mod_S", pron).posRoot().build();
  final MorphemeState pronAfterRel_S = builder("pronAfterRel_S", pron).posRoot().build();

  final MorphemeState pA1sg_S = nonTerminal("pA1sg_S", a1sg);
  final MorphemeState pA2sg_S = nonTerminal("pA2sg_S", a2sg);
  final MorphemeState pA1sgMod_S = nonTerminal("pA1sgMod_S", a1sg);
  final MorphemeState pA2sgMod_S = nonTerminal("pA2sgMod_S", a2sg);
  final MorphemeState pA3sg_S = nonTerminal("pA3sg_S", a3sg);
  final MorphemeState pA3sgRel_S = nonTerminal("pA3sgRel_S", a3sg);
  final MorphemeState pA1pl_S = nonTerminal("pA1pl_S", a1pl);
  final MorphemeState pA2pl_S = nonTerminal("pA2pl_S", a2pl);
  final MorphemeState pA3pl_S = nonTerminal("pA3pl_S", a3pl);
  final MorphemeState pA3plRel_S = nonTerminal("pA3plRel_S", a3pl);

  final MorphemeState pQuantA3sg_S = nonTerminal("pQuantA3sg_S", a3sg);
  final MorphemeState pQuantA3pl_S = nonTerminal("pQuantA3pl_S", a3pl);
  final MorphemeState pQuantModA3pl_S = nonTerminal("pQuantModA3pl_S", a3pl);
  final MorphemeState pQuantA1pl_S = nonTerminal("pQuantA1pl_S", a1pl);
  final MorphemeState pQuantA2pl_S = nonTerminal("pQuantA2pl_S", a2pl);

  final MorphemeState pQuesA3sg_S = nonTerminal("pQuesA3sg_S", a3sg);
  final MorphemeState pQuesA3pl_S = nonTerminal("pQuesA3pl_S", a3pl);

  final MorphemeState pReflexA3sg_S = nonTerminal("pReflexA3sg_S", a3sg);
  final MorphemeState pReflexA3pl_S = nonTerminal("pReflexA3pl_S", a3pl);
  final MorphemeState pReflexA1sg_S = nonTerminal("pReflexA1sg_S", a1sg);
  final MorphemeState pReflexA2sg_S = nonTerminal("pReflexA2sg_S", a2sg);
  final MorphemeState pReflexA1pl_S = nonTerminal("pReflexA1pl_S", a1pl);
  final MorphemeState pReflexA2pl_S = nonTerminal("pReflexA2pl_S", a2pl);

  final MorphemeState pPnon_S = nonTerminal("pPnon_S", pnon);
  final MorphemeState pPnonRel_S = nonTerminal("pPnonRel_S", pnon);
  final MorphemeState pPnonMod_S = nonTerminal("pPnonMod_S", pnon);
  final MorphemeState pP1sg_S = nonTerminal("pP1sg_S", p1sg);
  final MorphemeState pP2sg_S = nonTerminal("pP2sg_S", p2sg);
  final MorphemeState pP3sg_S = nonTerminal("pP3sg_S", p3sg);
  final MorphemeState pP1pl_S = nonTerminal("pP1pl_S", p1pl);
  final MorphemeState pP2pl_S = nonTerminal("pP2pl_S", p2pl);
  final MorphemeState pP3pl_S = nonTerminal("pP3pl_S", p3pl);

  final MorphemeState pNom_ST = terminal("pNom_ST", nom);
  final MorphemeState pDat_ST = terminal("pDat_ST", dat);
  final MorphemeState pAcc_ST = terminal("pAcc_ST", acc);
  final MorphemeState pAbl_ST = terminal("pAbl_ST", abl);
  final MorphemeState pLoc_ST = terminal("pLoc_ST", loc);
  final MorphemeState pGen_ST = terminal("pGen_ST", gen);
  final MorphemeState pIns_ST = terminal("pIns_ST", ins);
  final MorphemeState pEqu_ST = terminal("pEqu_ST", equ);
  final MorphemeState pronZeroDeriv_S = nonTerminalDerivative("pronZeroDeriv_S", zero);

  final MorphemeState pvPresent_S = nonTerminal("pvPresent_S", pres);
  final MorphemeState pvPast_S = nonTerminal("pvPast_S", past);
  final MorphemeState pvNarr_S = nonTerminal("pvNarr_S", narr);
  final MorphemeState pvCond_S = nonTerminal("pvCond_S", cond);
  final MorphemeState pvA1sg_ST = terminal("pvA1sg_ST", a1sg);
  final MorphemeState pvA2sg_ST = terminal("pvA2sg_ST", a2sg);
  final MorphemeState pvA3sg_ST = terminal("pvA3sg_ST", a3sg);
  final MorphemeState pvA3sg_S = nonTerminal("pvA3sg_S", a3sg);
  final MorphemeState pvA1pl_ST = terminal("pvA1pl_ST", a1pl);
  final MorphemeState pvA2pl_ST = terminal("pvA2pl_ST", a2pl);
  final MorphemeState pvA3pl_ST = terminal("pvA3pl_ST", a3pl);
  final MorphemeState pvCopBeforeA3pl_S = nonTerminal("pvCopBeforeA3pl_S", cop);
  final MorphemeState pvCop_ST = terminal("pvCop_ST", cop);
  final MorphemeState pvVerbRoot_S = builder("pvVerbRoot_S", verb).posRoot().build();

  //-------------- Adverbs, Minor POS & Postpositives ------------------------

  final MorphemeState advRoot_ST = builder("advRoot_ST", adv).posRoot().terminal().build();
  final MorphemeState advNounRoot_ST = builder("advNounRoot_ST", adv).posRoot().terminal().build();
  final MorphemeState advForVerbDeriv_ST =
      builder("advForVerbDeriv_ST", adv).posRoot().terminal().build();
  final MorphemeState avNounAfterAdvRoot_ST = builder("advToNounRoot_ST", noun).posRoot().build();
  final MorphemeState avA3sg_S = nonTerminal("avA3sg_S", a3sg);
  final MorphemeState avPnon_S = nonTerminal("avPnon_S", pnon);
  final MorphemeState avDat_ST = terminal("avDat_ST", dat);
  final MorphemeState avZero_S = nonTerminalDerivative("avZero_S", zero);
  final MorphemeState avZeroToVerb_S = nonTerminalDerivative("avZeroToVerb_S", zero);

  final MorphemeState conjRoot_ST = builder("conjRoot_ST", conj).posRoot().terminal().build();
  final MorphemeState interjRoot_ST = builder("interjRoot_ST", interj).posRoot().terminal().build();
  final MorphemeState detRoot_ST = builder("detRoot_ST", det).posRoot().terminal().build();
  final MorphemeState dupRoot_ST = builder("dupRoot_ST", dup).posRoot().terminal().build();

  final MorphemeState postpRoot_ST = builder("postpRoot_ST", postp).posRoot().terminal().build();
  final MorphemeState postpZero_S = nonTerminalDerivative("postpZero_S", zero);
  final MorphemeState po2nRoot_S = nonTerminal("po2nRoot_S", noun);
  final MorphemeState po2nA3sg_S = nonTerminal("po2nA3sg_S", a3sg);
  final MorphemeState po2nA3pl_S = nonTerminal("po2nA3pl_S", a3pl);
  final MorphemeState po2nP3sg_S = nonTerminal("po2nP3sg_S", p3sg);
  final MorphemeState po2nP1sg_S = nonTerminal("po2nP1sg_S", p1sg);
  final MorphemeState po2nP2sg_S = nonTerminal("po2nP2sg_S", p2sg);
  final MorphemeState po2nP1pl_S = nonTerminal("po2nP1pl_S", p1pl);
  final MorphemeState po2nP2pl_S = nonTerminal("po2nP2pl_S", p2pl);
  final MorphemeState po2nPnon_S = nonTerminal("po2nPnon_S", pnon);
  final MorphemeState po2nNom_ST = terminal("po2nNom_ST", nom);
  final MorphemeState po2nDat_ST = terminal("po2nDat_ST", dat);
  final MorphemeState po2nAbl_ST = terminal("po2nAbl_ST", abl);
  final MorphemeState po2nLoc_ST = terminal("po2nLoc_ST", loc);
  final MorphemeState po2nIns_ST = terminal("po2nIns_ST", ins);
  final MorphemeState po2nAcc_ST = terminal("po2nAcc_ST", acc);
  final MorphemeState po2nGen_ST = terminal("po2nGen_ST", gen);
  final MorphemeState po2nEqu_ST = terminal("po2nEqu_ST", equ);

  //-------------- Verb States ------------------------

  final MorphemeState verbRoot_S = builder("verbRoot_S", verb).posRoot().build();
  final MorphemeState verbLastVowelDropModRoot_S =
      builder("verbLastVowelDropModRoot_S", verb).posRoot().build();
  final MorphemeState verbLastVowelDropUnmodRoot_S =
      builder("verbLastVowelDropUnmodRoot_S", verb).posRoot().build();

  final MorphemeState vA1sg_ST = terminal("vA1sg_ST", a1sg);
  final MorphemeState vA2sg_ST = terminal("vA2sg_ST", a2sg);
  final MorphemeState vA3sg_ST = terminal("vA3sg_ST", a3sg);
  final MorphemeState vA1pl_ST = terminal("vA1pl_ST", a1pl);
  final MorphemeState vA2pl_ST = terminal("vA2pl_ST", a2pl);
  final MorphemeState vA3pl_ST = terminal("vA3pl_ST", a3pl);

  final MorphemeState vPast_S = nonTerminal("vPast_S", past);
  final MorphemeState vNarr_S = nonTerminal("vNarr_S", narr);
  final MorphemeState vCond_S = nonTerminal("vCond_S", cond);
  final MorphemeState vCondAfterPerson_ST = terminal("vCondAfterPerson_ST", cond);
  final MorphemeState vPastAfterTense_S = nonTerminal("vPastAfterTense_S", past);
  final MorphemeState vNarrAfterTense_S = nonTerminal("vNarrAfterTense_S", narr);
  final MorphemeState vPastAfterTense_ST = terminal("vPastAfterTense_ST", past);
  final MorphemeState vNarrAfterTense_ST = terminal("vNarrAfterTense_ST", narr);
  final MorphemeState vCond_ST = terminal("vCond_ST", cond);

  final MorphemeState vProgYor_S = nonTerminal("vProgYor_S", prog1);
  final MorphemeState vProgMakta_S = nonTerminal("vProgMakta_S", prog2);
  final MorphemeState vFut_S = nonTerminal("vFut_S", fut);

  final MorphemeState vCop_ST = terminal("vCop_ST", cop);
  final MorphemeState vCopBeforeA3pl_S = nonTerminal("vCopBeforeA3pl_S", cop);

  final MorphemeState vNeg_S = nonTerminal("vNeg_S", neg);
  final MorphemeState vUnable_S = nonTerminal("vUnable_S", unable);
  final MorphemeState vNegProg1_S = nonTerminal("vNegProg1_S", neg);
  final MorphemeState vUnableProg1_S = nonTerminal("vUnableProg1_S", unable);

  final MorphemeState vImp_S = nonTerminal("vImp_S", imp);
  final MorphemeState vImpYemekYi_S = nonTerminal("vImpYemekYi_S", imp);
  final MorphemeState vImpYemekYe_S = nonTerminal("vImpYemekYe_S", imp);

  final MorphemeState vCausT_S = nonTerminalDerivative("vCaus_S", caus);
  final MorphemeState vCausTır_S = nonTerminalDerivative("vCausTır_S", caus);

  final MorphemeState vRecip_S = nonTerminalDerivative("vRecip_S", recip);
  final MorphemeState vImplicitRecipRoot_S =
      builder("vImplicitRecipRoot_S", verb).posRoot().build();

  final MorphemeState vReflex_S = nonTerminalDerivative("vReflex_S", reflex);
  final MorphemeState vImplicitReflexRoot_S =
      builder("vImplicitReflexRoot_S", verb).posRoot().build();

  final MorphemeState verbRoot_VowelDrop_S =
      builder("verbRoot_VowelDrop_S", verb).posRoot().build();

  final MorphemeState vAor_S = nonTerminal("vAor_S", aor);
  final MorphemeState vAorNeg_S = nonTerminal("vAorNeg_S", aor);
  final MorphemeState vAorNegEmpty_S = nonTerminal("vAorNegEmpty_S", aor);
  final MorphemeState vAorPartNeg_S = nonTerminalDerivative("vAorPartNeg_S", aorPart);
  final MorphemeState vAorPart_S = nonTerminalDerivative("vAorPart_S", aorPart);

  final MorphemeState vAble_S = nonTerminalDerivative("vAble_S", able);
  final MorphemeState vAbleNeg_S = nonTerminalDerivative("vAbleNeg_S", able);
  final MorphemeState vAbleNegDerivRoot_S =
      builder("vAbleNegDerivRoot_S", verb).posRoot().build();

  final MorphemeState vPass_S = nonTerminalDerivative("vPass_S", pass);

  final MorphemeState vOpt_S = nonTerminal("vOpt_S", opt);
  final MorphemeState vDesr_S = nonTerminal("vDesr_S", desr);
  final MorphemeState vNeces_S = nonTerminal("vNeces_S", neces);

  final MorphemeState vInf1_S = nonTerminalDerivative("vInf1_S", inf1);
  final MorphemeState vInf2_S = nonTerminalDerivative("vInf2_S", inf2);
  final MorphemeState vInf3_S = nonTerminalDerivative("vInf3_S", inf3);
  final MorphemeState vAgt_S = nonTerminalDerivative("vAgt_S", agt);
  final MorphemeState vActOf_S = nonTerminalDerivative("vActOf_S", actOf);

  final MorphemeState vPastPart_S = nonTerminalDerivative("vPastPart_S", pastPart);
  final MorphemeState vFutPart_S = nonTerminalDerivative("vFutPart_S", futPart);
  final MorphemeState vPresPart_S = nonTerminalDerivative("vPresPart_S", presPart);
  final MorphemeState vNarrPart_S = nonTerminalDerivative("vNarrPart_S", narrPart);

  final MorphemeState vFeelLike_S = nonTerminalDerivative("vFeelLike_S", feelLike);
  final MorphemeState vNotState_S = nonTerminalDerivative("vNotState_S", notState);

  final MorphemeState vEverSince_S = nonTerminalDerivative("vEverSince_S", everSince);
  final MorphemeState vRepeat_S = nonTerminalDerivative("vRepeat_S", repeat);
  final MorphemeState vAlmost_S = nonTerminalDerivative("vAlmost_S", almost);
  final MorphemeState vHastily_S = nonTerminalDerivative("vHastily_S", hastily);
  final MorphemeState vStay_S = nonTerminalDerivative("vStay_S", stay);
  final MorphemeState vStart_S = nonTerminalDerivative("vStart_S", start);

  final MorphemeState vWhile_S = nonTerminalDerivative("vWhile_S", while_);
  final MorphemeState vWhen_S = nonTerminalDerivative("vWhen_S", when);
  final MorphemeState vAsIf_S = nonTerminalDerivative("vAsIf_S", asIf);
  final MorphemeState vSinceDoingSo_S = nonTerminalDerivative("vSinceDoingSo_S", sinceDoingSo);
  final MorphemeState vAsLongAs_S = nonTerminalDerivative("vAsLongAs_S", asLongAs);
  final MorphemeState vByDoingSo_S = nonTerminalDerivative("vByDoingSo_S", byDoingSo);
  final MorphemeState vAdamantly_S = nonTerminalDerivative("vAdamantly_S", adamantly);
  final MorphemeState vAfterDoing_S = nonTerminalDerivative("vAfterDoing_S", afterDoingSo);
  final MorphemeState vWithoutHavingDoneSo_S =
      nonTerminalDerivative("vWithoutHavingDoneSo_S", withoutHavingDoneSo);
  final MorphemeState vWithoutBeingAbleToHaveDoneSo_S =
      nonTerminalDerivative("vWithoutBeingAbleToHaveDoneSo_S", withoutBeingAbleToHaveDoneSo);

  final MorphemeState vDeYeRoot_S = builder("vDeYeRoot_S", verb).posRoot().build();

  //-------------- Question (mi) & Imek States ------------------------

  final MorphemeState qPresent_S = nonTerminal("qPresent_S", pres);
  final MorphemeState qPast_S = nonTerminal("qPast_S", past);
  final MorphemeState qNarr_S = nonTerminal("qNarr_S", narr);
  final MorphemeState qA1sg_ST = terminal("qA1sg_ST", a1sg);
  final MorphemeState qA2sg_ST = terminal("qA2sg_ST", a2sg);
  final MorphemeState qA3sg_ST = terminal("qA3sg_ST", a3sg);
  final MorphemeState qA1pl_ST = terminal("qA1pl_ST", a1pl);
  final MorphemeState qA2pl_ST = terminal("qA2pl_ST", a2pl);
  final MorphemeState qA3pl_ST = terminal("qA3pl_ST", a3pl);
  final MorphemeState qCopBeforeA3pl_S = nonTerminal("qCopBeforeA3pl_S", cop);
  final MorphemeState qCop_ST = terminal("qCop_ST", cop);
  final MorphemeState questionRoot_S = builder("questionRoot_S", ques).posRoot().build();

  final MorphemeState imekRoot_S = builder("imekRoot_S", verb).posRoot().build();
  final MorphemeState imekPast_S = nonTerminal("imekPast_S", past);
  final MorphemeState imekNarr_S = nonTerminal("imekNarr_S", narr);
  final MorphemeState imekCond_S = nonTerminal("imekCond_S", cond);
  final MorphemeState imekA1sg_ST = terminal("imekA1sg_ST", a1sg);
  final MorphemeState imekA2sg_ST = terminal("imekA2sg_ST", a2sg);
  final MorphemeState imekA3sg_ST = terminal("imekA3sg_ST", a3sg);
  final MorphemeState imekA1pl_ST = terminal("imekA1pl_ST", a1pl);
  final MorphemeState imekA2pl_ST = terminal("imekA2pl_ST", a2pl);
  final MorphemeState imekA3pl_ST = terminal("imekA3pl_ST", a3pl);
  final MorphemeState imekCop_ST = terminal("imekCop_ST", cop);
}
