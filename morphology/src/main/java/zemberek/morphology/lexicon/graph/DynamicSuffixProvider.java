package zemberek.morphology.lexicon.graph;

import com.google.common.collect.Maps;
import zemberek.morphology.lexicon.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DynamicSuffixProvider implements SuffixProvider {

    protected Map<SuffixForm, SuffixForm> suffixForms = Maps.newHashMap();
    protected Map<String, Suffix> suffixLookup = Maps.newHashMap();
    protected Map<String, SuffixForm> formLookupByName = Maps.newHashMap();
    private Map<NullSuffixForm, NullSuffixForm> nullFormsUnprocessed = Maps.newHashMap();

    protected IdMaker idMaker = new IdMaker(3);
    protected AtomicInteger indexMaker = new AtomicInteger();

    public NullSuffixForm getNull(String suffixId, SuffixFormTemplate template) {
        return new NullSuffixForm(getNewIndex(), suffixId, template);
    }

    public NullSuffixForm getNull(String suffixId, SuffixFormTemplate template, TerminationType type) {
        return new NullSuffixForm(getNewIndex(), suffixId, template, type);
    }

    public SuffixForm getForm(Suffix suffix, String generationStr) {
        return new SuffixForm(getNewIndex(), suffix, generationStr);
    }

    public SuffixForm getForm(String id, Suffix suffix, String generationStr) {
        return new SuffixForm(getNewIndex(), id, suffix, generationStr);
    }

    public SuffixForm getForm(String id, Suffix suffix, String generationStr, TerminationType type) {
        return new SuffixForm(getNewIndex(), id, suffix, generationStr, type);
    }

    public SuffixFormTemplate getTemplate(String id, Suffix suffix) {
        return new SuffixFormTemplate(getNewIndex(), id, suffix);
    }

    public SuffixFormTemplate getTemplate(String id, Suffix suffix, TerminationType type) {
        return new SuffixFormTemplate(getNewIndex(), id, suffix, type);
    }

    public DerivationalSuffixTemplate getDerivationalTemplate(String id, Suffix suffix, TerminationType type) {
        return new DerivationalSuffixTemplate(getNewIndex(), id, suffix, type);
    }

    private int getNewIndex() {
        return indexMaker.getAndIncrement();
    }

    public Suffix getSuffixById(String suffixId) {
        return suffixLookup.get(suffixId);
    }

/*    public SuffixForm getSuffixFormById(String suffixId) {
        return formLookupByName.get(suffixId);
    } */

    public Iterable<SuffixForm> getAllForms() {
        return suffixForms.keySet();
    }

    public int getFormCount() {
        return suffixForms.size();
    }

    @Override
    public abstract SuffixData[] defineSuccessorSuffixes(DictionaryItem item);

    @Override
    public abstract SuffixForm getRootSet(DictionaryItem item,  SuffixData successors);

    protected void registerForms(SuffixForm... setz) {
        for (SuffixForm formSet : setz) {
            registerForm(formSet);
        }
    }

    protected NullSuffixForm generateNullFormFromTemplate(SuffixFormTemplate templateForm, SuffixData constraints) {
        NullSuffixForm nullForm = new NullSuffixForm(-1, "", templateForm);
        nullForm.connections = new SuffixData(templateForm.connections).retain(constraints);
        nullForm.indirectConnections = new SuffixData(templateForm.indirectConnections).retain(constraints);

        if (nullFormsUnprocessed.containsKey(nullForm)) {
            return nullFormsUnprocessed.get(nullForm);
        } else {
            nullForm.index = getNewIndex();
            nullForm.id = idMaker.get(templateForm.id);
            nullFormsUnprocessed.put(nullForm, nullForm);
            return nullForm;
        }
    }

    protected void registerForm(SuffixForm formSet) {

        // if this is a template, we put basic template data to a lookup table. we will use this table later to detect
        // duplicates of newly generated FormSets.
        if (formSet instanceof SuffixFormTemplate) {
            formLookupByName.put(formSet.getId(), formSet);
            return;
        }

        if (suffixForms.containsKey(formSet)) {
            return;
        }

        SuffixData allConnections = formSet.allConnections();

        List<SuffixForm> templateFormsToRemove = new ArrayList<>();
        List<SuffixForm> nullFormsToRegister = new ArrayList<>();
        for (SuffixForm connection : formSet.connections) {
            if (connection instanceof SuffixFormTemplate) {
                NullSuffixForm nullForm =
                        generateNullFormFromTemplate(
                                (SuffixFormTemplate) connection,
                                new SuffixData(allConnections)).copy();
                nullFormsToRegister.add(nullForm);
                templateFormsToRemove.add(connection);
            }
        }

        formSet.connections.remove(templateFormsToRemove);
        // we dont need indirect connection data anymore.
        formSet.indirectConnections.clear();
        formSet.connections.add(nullFormsToRegister);

        if (formSet.index != -1)
            formSet.index = getNewIndex();

        suffixForms.put(formSet, formSet);
        formLookupByName.put(formSet.getId(), formSet);

        for (SuffixForm form : nullFormsToRegister) {
            registerForm(form);
        }
    }

    public SuffixForm getSuffixFormById(String id) {
        return formLookupByName.get(id);
    }

    public void dumpPath(SuffixForm set, int level) {
        if (level == 0)
            return;
        System.out.println("--------------------------SET:" + set.id);
        System.out.println("D:");
        for (SuffixForm direct : set.connections) {
            System.out.println(direct.id);
        }
        System.out.println("S:");
        for (SuffixForm sec : set.indirectConnections) {
            System.out.println(sec.id);
        }
        for (SuffixForm direct : set.connections) {
            dumpPath(direct, level - 1);
        }
    }
}
