package org.bme.mit.iir;

import java.io.File;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class ReasoningExample {

	public static final String PCSHOP_ONTOLOGY_FNAME =
		"../res/pc_shop.owl_modified.xml";
	public static final String PCSHOP_BASE_URI = 
		"http://mit.bme.hu/ontologia/iir_labor/pc_shop.owl#";
    public static final IRI ANNOTATION_LABEL_IRI =
            OWLRDFVocabulary.RDFS_LABEL.getIRI();
    public static final IRI ANNOTATION_COMMENT_IRI =
            OWLRDFVocabulary.RDFS_COMMENT.getIRI();

    OWLOntologyManager manager;
    OWLOntology ontology;
    OWLReasoner reasoner;
    OWLDataFactory factory;
    
    public ReasoningExample(String ontologyFilename) {
        // Hozzunk létre egy OntologyManager példányt. Többek között ez tartja 
    	// nyilván, hogy mely ontológiákat nyitottuk meg és azok hogyan 
    	// hivatkoznak egymásra. 
        manager = OWLManager.createOWLOntologyManager();

        // Töltsük be az ontológiát egy fizikai címről.
        // [Az ontológiáknak külön van logikai és fizikai címe, a kettő közötti 
        // leképezést az URIMapper-ek határozzák meg: manager.addURIMapper(...).
        // Erre ontológiák közötti függőségek (import) kialakításánál van 
        // szükség.]
        ontology = null;
		try {
			ontology = manager.loadOntologyFromOntologyDocument(
					new File(ontologyFilename));
		} catch (Exception e) {
			System.err.println("Hiba az ontológia betöltése közben:\n\t"
					+ e.getMessage());
			System.exit(-1);
		}
        System.out.println("Ontológia betöltve: " + 
                manager.getOntologyDocumentIRI(ontology));

        // Létrehozzuk a következtetőgép egy példányát. Mi most a HermiT-et 
        // használjuk, de az OWL API univerzális, másik következtető 
        // használatához csak a Factory osztály nevét kellene kicserélni.
        
        OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
        //
        // Hozzunk létre egy következtetőgépet, betöltve az ontológiát.
        // [Ha vannak az ontológiának függőségei, azok automatikusan
        // betöltődnek a manager segítségével.]
        reasoner = reasonerFactory.createReasoner(ontology);

        try {
	        // Ha az ontológia nem konzisztens, lépjünk ki.
	        if (!reasoner.isConsistent()) {
	        	System.err.println("Az ontológia nem konzisztens!");
	        	
	            Node<OWLClass> incClss = reasoner.getUnsatisfiableClasses();
                System.err.println("A következő osztályok nem konzisztensek: "
                    + Util.join(incClss.getEntities(), ", ") + ".");
	        	System.exit(-1);
	        }
		} catch (OWLReasonerRuntimeException e) {
			System.err.println("Hiba a következtetőben: " + e.getMessage());
			System.exit(-1);
		}

        // Példányosítsuk az OWLDataFactory-t, aminek a segítségével létre tudunk
        // hozni URI alapján OWL entitásokat (osztályt, tulajdonságot, stb).
        factory = manager.getOWLDataFactory();
    }
    
    // Lekérdezi egy osztály leszármazottait a PC-Shop ontológiában.
    //   className: a keresett osztály neve
    // Az OWL következtető Set<Set<..>> struktúrákban, azaz halmazok 
    // halmazaként adja vissza a lekérdezés eredményét. A belső halmazok 
    // egymással ekvivalens, jelentés szempontjából megegyező dolgokat 
    // tartalmaznak. Például egy lekérdezés eredménye:
    // { { Kutya, Eb }, { Macska, Cica }, { Ló } }
    // Ez a függvény a fenti halmazt "kilapítva" adja vissza:
    // { Kutya, Eb, Macska, Cica, Ló }
    public Set<OWLClass> getSubClasses(String className, boolean direct) {
        OWLClass cls = getOWLEntityOf(className);

        if(cls == null){
            return Collections.emptySet();
        }
        NodeSet<OWLClass> subClss;
		try {
			subClss = reasoner.getSubClasses(cls, direct);
		} catch (OWLReasonerRuntimeException e) {
			System.err.println("Hiba az alosztályok következtetése közben: "
					+ e.getMessage());
			return Collections.emptySet();
		}
		// Kiírjuk az eredményt, az ekvivalens osztályokat 
		// egyenlőségjellel elválasztva.
        System.out.println("Az \"" + className + "\" osztály leszármazottai:");
        for (Node<OWLClass> subCls : subClss.getNodes()) {
        	System.out.println("  - " + Util.join(subCls.getEntities(), " = "));
        }
        return subClss.getFlattened();
    }

    // Lekérdezi egy OWL entitás (osztály, tulajdonság vagy egyed) annotációit
    // az ontológiából. (Ehhez nincs szükség a következtetőre.)
    // Az annotációknak sok fajtája van, a lekérdezett típust az
    // ANNOTATION_TYPE_IRI konstans tartalmazza, jelen esetben rdfs:label.
    // Kiegészítve: adja vissza az rdfs:comment típusú annotációkat is
    public Set<String> getClassAnnotations(OWLEntity entity) {
        OWLAnnotationProperty label =
                factory.getOWLAnnotationProperty(ANNOTATION_LABEL_IRI);

        OWLAnnotationProperty comment =
                factory.getOWLAnnotationProperty(ANNOTATION_COMMENT_IRI);

        Set<String> result = new HashSet<String>();
        for (OWLAnnotation a : entity.getAnnotations(ontology, label)) {
            if (a.getValue() instanceof OWLLiteral) {
                OWLLiteral value = (OWLLiteral)a.getValue();
                result.add(value.getLiteral());
            }
        }
        for (OWLAnnotation a : entity.getAnnotations(ontology, comment)) {
            if (a.getValue() instanceof OWLLiteral) {
                OWLLiteral value = (OWLLiteral)a.getValue();
                result.add(value.getLiteral());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public OWLClass getOWLEntityOf(String className){
        // Létrehozzuk az osztály URI-ját a base URI alapján.
        IRI clsIRI = IRI.create(PCSHOP_BASE_URI + className);
        // Ellenőrizzük, hogy az osztály szerepel-e az ontológiában.
        // (Ha olyan dologra kérdezünk rá a következtetővel, ami nem szerepel az
        // ontológiában, az nem vezet hibához az OWL nyílt világ feltételezése
        // miatt.)
        if (!ontology.containsClassInSignature(clsIRI)) {
            System.out.println("Nincs ilyen osztály az ontológiában: \"" +
                    className + "\"");
            return null;
        }
        // Létrehozzuk az osztály egy példányát és lekérdezzük a leszármazottait.
        return factory.getOWLClass(clsIRI);
    }

    // Példaprogram: beolvassa a pc-shop OWL ontológiát, majd listázza
    // az "alkatrész" osztály valamennyi leszármazottját és azok annotációit.
    public static void main(String[] args) {
    	// Példányosítsuk a fenti egyszerű Pellet következtető osztályt.
    	ReasoningExample p = new ReasoningExample(
    			PCSHOP_ONTOLOGY_FNAME);
    	
        // Végezzük keresőszó-kiegészítést az "alkatrész" kulcsszóra 
    	// az osztály leszármazottai szerint!
    	final String term = "áru";
    	Set<OWLClass> descendants = p.getSubClasses(term, false);
        System.out.println("Query expansion a leszármazottak szerint: ");
    	for (OWLClass cls : descendants) {
    		// Az eredmények közül a beépített OWL entitásokat ki kell szűrnünk.
    		// Ezek itt az osztályhierarchia tetejét és alját jelölő
    		// "owl:Thing" és "owl:Nothing" lehetnek.
    		if (!cls.isBuiltIn()) {
                // Kérdezzük le az osztály címkéit (annotation rdfs:label).
                Set<String> labels = p.getClassAnnotations(cls);
    			System.out.println("\t- "
    					+ term + " -> " + cls.getIRI().getFragment()
                        + " [" + Util.join(labels, ", ") + "]");
            }
    	}

    }

    public List<String> getQueryExpansion(String term) {
        ArrayList<String> expansion = new ArrayList<>();

        // Példányosítsuk a fenti egyszerű Pellet következtető osztályt.
        ReasoningExample p = new ReasoningExample(
                PCSHOP_ONTOLOGY_FNAME);

        // Végezzük keresőszó-kiegészítést az "alkatrész" kulcsszóra
        // az osztály leszármazottai szerint!

        // Amennyiben az ontológia tartalmazza a keresett szót, vegye fel ennek annotációit is.
        var owlObject = getOWLEntityOf(term);
        if(owlObject != null) {
            Set<String> lbls = p.getClassAnnotations(owlObject);

            for (var label : lbls) {
                expansion.add(label);
            }
        }

        Set<OWLClass> descendants = p.getSubClasses(term, false);
        System.out.println("Query expansion a leszármazottak szerint: ");
        for (OWLClass cls : descendants) {
            // Az eredmények közül a beépített OWL entitásokat ki kell szűrnünk.
            // Ezek itt az osztályhierarchia tetejét és alját jelölő
            // "owl:Thing" és "owl:Nothing" lehetnek.
            if (!cls.isBuiltIn()) {
                expansion.add(cls.getIRI().getFragment());
                // Kérdezzük le az osztály címkéit (annotation rdfs:label).
                Set<String> labels = p.getClassAnnotations(cls);

                for(var label : labels){
                    System.out.println("label: "+label);
                    expansion.add(label);
                }
            }
        }
        return expansion;
    }

}
