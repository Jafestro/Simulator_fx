package simu.model;

import simu.framework.*;
import eduni.distributions.Negexp;
import eduni.distributions.Normal;
import controller.IKontrolleriForM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class OmaMoottori extends Moottori {

    private Saapumisprosessi saapumisprosessi;

    private HashMap<String, ArrayList<Palvelupiste>> palvelupisteet = new HashMap<>();
    private int prosentti;

    public OmaMoottori(IKontrolleriForM kontrolleri) {

        super(kontrolleri);

        for (int i = 0; i < 4; i++) {
            palvelupisteet.put(Integer.toString(i), new ArrayList<>());
        }
        palvelupisteet.get("0").add(new Palvelupiste(new Normal(10, 6), tapahtumalista, TapahtumanTyyppi.INFOTISKI));
        palvelupisteet.get("1").add(new Palvelupiste(new Normal(10, 10), tapahtumalista, TapahtumanTyyppi.UUDEN_TILIN_AVAUS));
        palvelupisteet.get("2").add(new Palvelupiste(new Normal(5, 3), tapahtumalista, TapahtumanTyyppi.TALLETUS));
        palvelupisteet.get("3").add(new Palvelupiste(new Normal(6, 9), tapahtumalista, TapahtumanTyyppi.SIJOITUS_PALVELUT));
        saapumisprosessi = new Saapumisprosessi(new Negexp(15, 5), tapahtumalista, TapahtumanTyyppi.SAAPUMINEN);

    }

    public void addPalvelu(String type, String count) {

    }

    public void deletePalvelu(String type, String count) {

    }

    public void setProsentti(int uusiProsentti) {
        prosentti = uusiProsentti;
    }

    @Override
    protected void alustukset() {
        saapumisprosessi.generoiSeuraava(); // Ensimmäinen saapuminen järjestelmään
    }

    //TODO Check if in suoritaTapahtuma in if else logic smth wrong or not
    @Override
    protected void suoritaTapahtuma(Tapahtuma t) {  // B-vaiheen tapahtumat

        ArrayList<Asiakas> a;
        switch ((TapahtumanTyyppi) t.getTyyppi()) {

            case SAAPUMINEN:
                Asiakas as = new Asiakas(generateTrueFalse()); // uusi Asiakas saapui
                System.out.println(as.isOnVarattu()); // debug
                if (as.isOnVarattu())
                    lisaaJonoon(Byte.toString(as.getTavoite()), "lisaaVarattuJonoon", as);
                else
                    lisaaJonoon("0", "lisaaJonoon", as);
                kontrolleri.visualisoiAsiakas();
                saapumisprosessi.generoiSeuraava();
                break;
            case INFOTISKI: // 0
                a = otaJonosta("0", "otaJonosta");
                for (Asiakas asiakas : a){
                    if (asiakas.getTavoite() == 0) {
                        asiakas.setPoistumisaika(Kello.getInstance().getAika());
                        asiakas.raportti();
                        a.remove(asiakas);
                    }
                }
                if (!a.isEmpty()){
                    for (Asiakas asiakas : a ) {
                        lisaaJonoon(Byte.toString(asiakas.getTavoite()), "lisaaJonoon", asiakas);
                    }
                }
                break;
            case UUDEN_TILIN_AVAUS: // 1
                a = otaJonosta("1", "otaVarattuJonosta");
                handleCustomers("2", a, true);
                a = otaJonosta("1", "otaJonosta");
                handleCustomers("2", a, false);
                break;
            case TALLETUS:  // 2
                a = otaJonosta("2", "otaVarattuJonosta");
                handleCustomers("3", a, true);
                a = otaJonosta("2", "otaJonosta");
                handleCustomers("3", a, false);
                break;
            case SIJOITUS_PALVELUT: // 3
                a = otaJonosta("3", "otaVarattuJonosta");
                handleCustomers("4", a, true);
                a = otaJonosta("3", "otaJonosta");
                handleCustomers("4", a, false);
                break;
        }
    }


    // C-vaiheen tapahtumat

    // Tässä vaiheessa palvelupisteet käyvät läpi jononsa ja aloittavat palvelun
    // jos asiakas on jonossa ja palvelupiste on vapaa
    // tai jos asiakas on varattu jonossa ja palvelupiste on vapaa
    // Tämä metodi kutsutaan aina kun kello etenee
    @Override
    protected void yritaCTapahtumat() {
        palvelupisteet.forEach((k, v) -> {
            for (Palvelupiste p : v) {
                if (p.onJonossa() && !p.onVarattu()) {
                    p.aloitaPalvelu(false);
                } else if (p.onVarattuJonossa() && !p.onVarattu()) {
                    p.aloitaPalvelu(true);
                }
            }
        });
    }

    @Override
    protected void tulokset() {
        System.out.println("Simulointi päättyi kello " + Kello.getInstance().getAika());
        System.out.println("---------------------------------------------------------");
        System.out.println("Keskimääräinen läpikulku aika on:  " + Asiakas.getAverageTimeSpent());
        System.out.println("Asiakkaita palveltu: " + Asiakas.getTotalCustomers());
        System.out.println("Keskimääräinen asiakastyytyväisyys: " + Asiakas.getHappyRating());


        // UUTTA graafista
        kontrolleri.naytaLoppuaika(Kello.getInstance().getAika(), Asiakas.getHappyRating());
    }

    protected boolean generateTrueFalse() {
        Random random = new Random();
        double rn = random.nextDouble() * 100;
        System.out.println("Random: " + rn + " Prosentti: " + getVaratutProsentti());
        return rn <= getVaratutProsentti() || getVaratutProsentti() == 100;
    }

    protected void lisaaJonoon(String palvelupisteNmr, String cmd, Asiakas asiakas) {
        switch (cmd) {
            case "lisaaJonoon":
                palvelupisteet.get(palvelupisteNmr).stream()
                        .reduce((a, b) -> a.getJononPituus() < b.getJononPituus() ? a : b)
                        .get().lisaaJonoon(asiakas);
                break;
            case "lisaaVarattuJonoon":
                palvelupisteet.get(palvelupisteNmr).stream()
                        .reduce((a, b) -> a.getVaratunJononPituus() < b.getVaratunJononPituus() ? a : b)
                        .get().lisaaVarattuJonoon(asiakas);
                break;
        }
    }

    protected ArrayList<Asiakas> otaJonosta(String palvelupisteNmr, String cmd) {
        ArrayList <Asiakas> asiakkaat = new ArrayList<>();
        switch (cmd) {
            case "otaJonosta":
                for (Palvelupiste p : palvelupisteet.get(palvelupisteNmr)) {
                    if (p.onJonossa() && !p.onVarattu()) {
                        asiakkaat.add(p.otaJonosta());
                    }
                }
                break;
            case "otaVarattuJonosta":
                for (Palvelupiste p : palvelupisteet.get(palvelupisteNmr)) {
                    if (p.onVarattuJonossa()) {
                        asiakkaat.add(p.otaVarattuJonosta());
                    }
                }
                break;
        }
        return asiakkaat;
    }

    protected void handleCustomers(String palvelupisteNmr, ArrayList<Asiakas> a, boolean varattu) {
        Iterator<Asiakas> iterator = a.iterator();
        while (iterator.hasNext()) {
            Asiakas asiakas = iterator.next();
            if (varattu) {
                if (new Random().nextBoolean() && !palvelupisteNmr.equals("4")) {
                    lisaaJonoon(palvelupisteNmr, "lisaaVarattuJonoon", asiakas);
                } else {
                    asiakas.setPoistumisaika(Kello.getInstance().getAika());
                    asiakas.raportti();
                }
            } else {
                if (new Random().nextBoolean() && !palvelupisteNmr.equals("4")) {
                    lisaaJonoon(palvelupisteNmr, "lisaaJonoon", asiakas);
                } else {
                    asiakas.setPoistumisaika(Kello.getInstance().getAika());
                    asiakas.raportti();
                }
            }
            iterator.remove();
        }
    }
}
