package searchengine.services;
import jakarta.annotation.PostConstruct;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class LemmaParser {
    private LuceneMorphology russianMorph;
    private LuceneMorphology englishMorph;

    private static final String[] RUSSIAN_SERVICE_PARTS = {"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};
    private static final String[] ENGLISH_SERVICE_PARTS = {"PREP", "CONJ", "ARTICLE", "PART"};

    public LemmaParser() {

    }

    @PostConstruct
    public void init() {
        try {
            this.russianMorph = new RussianLuceneMorphology();
            this.englishMorph = new EnglishLuceneMorphology();
            System.out.println("DEBUG: LemmaParser успешно инициализировал словари.");
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Не удалось загрузить словари морфологии!");
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> getLemmaCount(String text) {
        String[] words = text.toLowerCase()
                .replaceAll("([^а-яёa-z0-9\\s])", " ")
                .trim()
                .split("\\s+");

        HashMap<String, Integer> lemmasMap = new HashMap<>();

        for (String word : words) {
            if (word.isBlank() || word.length() <= 1) continue;

            boolean isRus = isRussian(word);
            LuceneMorphology currentMorph = isRus ? russianMorph : englishMorph;
            String[] serviceParts = isRus ? RUSSIAN_SERVICE_PARTS : ENGLISH_SERVICE_PARTS;

            try {
                List<String> wordBaseForms = currentMorph.getMorphInfo(word);
                if (isServicePart(wordBaseForms, serviceParts)) {
                    continue;
                }
                List<String> normalForms = currentMorph.getNormalForms(word);
                if (normalForms.isEmpty()) continue;

                String lemma = normalForms.get(0);
                lemmasMap.put(lemma, lemmasMap.getOrDefault(lemma, 0) + 1);

            } catch (Exception e) {
            }
        }
        return lemmasMap;
    }


    private boolean isServicePart(List<String> wordBaseForms, String[] serviceParts) {
        return wordBaseForms.stream().anyMatch(info ->
                Arrays.stream(serviceParts).anyMatch(info::contains));
    }

    private boolean isRussian(String word) {
        return word.matches("[а-яё-]+");
    }

    public String clearHtmlFromTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String text = Jsoup.parse(html).text();
        return text;
    }

    public String getTitle(String html) {
        String title = Jsoup.parse(html).title();
        return (title == null || title.isEmpty()) ? "Без заголовка" : title;
    }

    public String createSnippet(String content, Map<String, Integer> queryLemmas) {
        String cleanText = clearHtmlFromTags(content)
                .replaceAll("\\s+", " ")
                .trim();

        String[] sentences = cleanText.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (String sentence : sentences) {
            if (sentence.contains("О нас в СМИ") || sentence.contains("Правила библиотеки")) continue;
            if (sentence.length() < 40) continue;

            boolean found = false;
            String highlighted = sentence;

            for (String lemma : queryLemmas.keySet()) {
                int cutOff = (lemma.length() > 5) ? 2 : (lemma.length() > 3 ? 1 : 0);
                String core = lemma.substring(0, lemma.length() - cutOff).toLowerCase();

                String regex;
                if (core.length() <= 3) {
                    regex = "(?iu)(?<![а-яёa-z])(" + core + "[а-яёa-z]{0,2})(?![а-яёa-z])";
                } else {
                    regex = "(?iu)(?<![а-яёa-z])(" + core + "[а-яёa-z]*)(?![а-яёa-z])";
                }

                if (highlighted.toLowerCase().contains(core)) {
                    highlighted = highlighted.replaceAll(regex, "<b>$1</b>");
                    if (highlighted.contains("<b>")) {
                        found = true;
                    }
                }
            }

            if (found) {
                String finalSent = highlighted.length() > 200 ? highlighted.substring(0, 197) + "..." : highlighted;
                sb.append(finalSent).append("... ");
                count++;
            }
            if (count >= 2) break;
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) {
            String fallback = cleanText.substring(0, Math.min(cleanText.length(), 150)) + "...";
            for (String lemma : queryLemmas.keySet()) {
                int cutOff = (lemma.length() > 5) ? 2 : (lemma.length() > 3 ? 1 : 0);
                String core = lemma.substring(0, lemma.length() - cutOff).toLowerCase();
                String regex = "(?iu)(?<![а-яёa-z])(" + core + "[а-яёa-z]*)(?![а-яёa-z])";
                fallback = fallback.replaceAll(regex, "<b>$1</b>");
            }
            return fallback;
        }
        return result;
    }

}