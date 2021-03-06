package com.unknownpotato.ohtu.miniproj.ui;

import com.unknownpotato.ohtu.miniproj.domain.FieldValidator;
import com.unknownpotato.ohtu.miniproj.domain.Reference;
import com.unknownpotato.ohtu.miniproj.domain.ReferenceFilters;
import com.unknownpotato.ohtu.miniproj.domain.ReferenceType;
import com.unknownpotato.ohtu.miniproj.domain.ReferenceUtils;
import com.unknownpotato.ohtu.miniproj.domain.References;
import com.unknownpotato.ohtu.miniproj.io.BibtexFormatter;
import com.unknownpotato.ohtu.miniproj.io.FileWriterHandler;
import com.unknownpotato.ohtu.miniproj.io.IO;
import com.unknownpotato.ohtu.miniproj.io.JSONReader;
import com.unknownpotato.ohtu.miniproj.io.JSONWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Terminal User Interface for the software.
 */
@Component
public class TextUI {

    /**
     * Handles reading input and writing output for the class.
     */
    private final IO io;
    /**
     * References handles the references stored in memory.
     */
    private References references;
    private final String DEFAULT_FILENAME = "references.json";

    /**
     *
     * @param references References -class that references created are stored
     * in.
     * @param io Input and output class.
     */
    @Autowired
    public TextUI(References references, IO io) {
        this.references = references;
        this.io = io;
    }

    /**
     * Start the reference handler UI.
     */
    public void run() {
        Map<String, Runnable> choices = setUpChoices();
        io.println("Welcome to BibTeX-reference formatter!");
        io.println("Input help to see commands.");
        while (true) {
            String choice = io.readCharacter("(main):");
            choice = choice.toLowerCase();

            if (choice.equals("q")) {
                return;
            }

            if (!choices.containsKey(choice)) {
                continue;
            }

            choices.get(choice).run();
        }
    }

    /**
     * Sets up the user interface commands.
     *
     * @return hashmap with the choices.
     */
    private Map<String, Runnable> setUpChoices() {
        final Map<String, Runnable> choices = new HashMap<>();
        choices.put("h", () -> listCommands());
        choices.put("a", () -> addReference());
        choices.put("l", () -> listReferences());
        choices.put("e", () -> editReference());
        choices.put("d", () -> deleteReference());
        choices.put("f", () -> manageFilters());
        choices.put("x", () -> exportToBibTex());
        choices.put("s", () -> saveReferences());
        choices.put("o", () -> loadReferences());
        return choices;
    }

    /**
     * Prints list of commands
     */
    private void listCommands() {
        io.println("[A]dd new reference,\n"
                + "[L]ist all references,\n"
                + "[E]dit a reference,\n"
                + "[D]elete a reference,\n"
                + "[F]ilter references,\n"
                + "e[X]port to BibTeX,\n"
                + "[S]ave references JSON file,\n"
                + "[O]pen references JSON file,\n"
                + "[Q]uit");
    }

    /**
     * Reads reference information from the user using IO and adds a new
     * reference to IO.
     */
    private void addReference() {
        ReferenceType type = chooseReferenceType();
        createNewReference(type);
        io.println("You have added a new reference!");
    }

    /**
     * Lists all choices on creating a new Reference.
     *
     * @return choices as string.
     */
    private void listAvailableReferenceTypes() {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < ReferenceType.values().length; i++) {
            if (i != 0 && (i) % 4 == 0){
                sb.append('\n');
            }
            sb.append(i).append("=").append(ReferenceType.values()[i]).append('\t');
            if (ReferenceType.values()[i].toString().length() < 6)
                sb.append('\t');
        }
        io.println(sb.toString());
    }

    private ReferenceType chooseReferenceType() {
        io.println("Input type of reference to add:");
        listAvailableReferenceTypes();
        while (true) {
            int i = io.readInt(":");
            if (i < 0 || i >= ReferenceType.values().length) {
                io.println("Invalid choice");
                continue;
            }
            return ReferenceType.values()[i];
        }
    }

    /**
     * Prints all references stored in references-object.
     */
    private void listReferences() {
        if (!references.getFilters().isEmpty()) {
            int filters = references.getFilters().size();
            if (filters == 1) {
                io.println(filters + " filter applied");
            } else {
                io.println(filters + " filters applied");
            }
        }
        if (references.getReferences().isEmpty()) {
            io.println("No references found!");
            return;
        }
        references.getReferences().stream().forEach(r -> {
            io.println("All references:");
            io.println(ReferenceUtils.referenceToString(r));
        });
    }

    /**
     * Deletes a Reference from references based input read from IO.
     */
    private void deleteReference() {
        String name = io.readLine("Name the reference to be deleted:\n");
        if (references.deleteReference(name)) {
            io.println("Reference " + name + " was deleted!");
        } else {
            io.println("Reference " + name + " was not found!");
        }
    }

    private Reference findReference() {
        io.println("Name the reference to be edited");
        String name = io.readLine(":");
        if (!references.contains(name)) {
            io.println("Reference " + name + " was not found!");
            return null;
        }
        return references.getReference(name);
    }

    private void editReference() {
        Reference ref = findReference();
        if (ref == null) {
            return;
        }

        Map<String, Consumer<Reference>> editChoices = setUpEditingChoices();

        io.println("[e]dit fields, [a]dd or edit a single field, [t]ag, [r]emove tags, [q]uit");
        String character = io.readCharacter("(" + ref.getName() + ") ");

        if (character.equals("q")) {
            return;
        }

        editChoices.get(character).accept(ref);
    }

    private Map<String, Consumer<Reference>> setUpEditingChoices() {
        final Map<String, Consumer<Reference>> editingChoices = new HashMap<>();
        editingChoices.put("e", param -> editFields(param));
        editingChoices.put("a", param -> addOrEditField(param));
        editingChoices.put("t", param -> addTags(param));
        editingChoices.put("r", param -> removeTags(param));
        return editingChoices;
    }

    private void editFields(Reference ref) {
        Map<String, String> fields = ref.getFields();

        io.println("Fill required fields");
        askForFields(Arrays.asList(ref.getType().getRequiredFields()), fields, false);

        if (askForPermission("Fill optional fields? ([Y]es/[N]o):")) {
            io.println("Fill optional fields, press enter to leave field empty.");
            askForFields(Arrays.asList(ref.getType().getOptionalFields()), fields, true);
        }

        ref.editFields(fields);
    }

    private void addOrEditField(Reference ref) {
        io.println("Name the field to be edited/added");
        String fieldToEdit = io.readLine(":");

        if (!ref.getFieldKeys().contains(fieldToEdit)) {
            io.println("The field " + fieldToEdit + " was not found!");
            return;
        }

        String newFieldContent = io.readLine(fieldToEdit + " [" + ref.getField(fieldToEdit) + "]:");
        ref.editField(fieldToEdit, newFieldContent);
        io.println("The field " + fieldToEdit + " was edited!");
    }

    /**
     * Exports references to running directory file BibTex_export.bib.
     */
    private void exportToBibTex() {
        if (references.getReferences().isEmpty()) {
            io.println("No references found!");
            return;
        }
        try {
            FileWriterHandler writer = new FileWriterHandler("BibTex_export.bib");
            writer.writeTo(references.getReferences().stream()
                    .map(r -> BibtexFormatter.convertReference(r))
                    .collect(Collectors.toList()));
        } catch (IOException ex) {
            io.println("Export failed!");
            return;
        }
        io.println("Export complete!");
    }

    /**
     * Creates a new Reference of Type type
     *
     * @param type type of reference created
     */
    private void createNewReference(ReferenceType type) {
        Map<String, String> fields = new HashMap<>();

        io.println("Fill required fields");
        askForFields(Arrays.asList(type.getRequiredFields()), fields, false);

        if (askForPermission("Fill optional fields? ([Y]es/[N]o):")) {
            io.println("Fill optional fields, press enter to leave field empty.");
            askForFields(Arrays.asList(type.getOptionalFields()), fields, true);
        }

        Reference ref = Reference.createReference(type, "", fields);
        references.addReference(ref);
    }

    /**
     * Asks for values to fields given in List fieldKeys.
     *
     * @param fieldKeys keys that values are asked for
     * @param fields where the value-key pairs are stored
     * @param canLeaveEmpty whether it's okay to leave a field empty
     */
    private void askForFields(List<String> fieldKeys, Map<String, String> fields, boolean canLeaveEmpty) {
        fieldKeys.stream().forEach(f -> {
            String oldValue = fields.get(f) != null ? fields.get(f) : "";
            String value = io.readLine(f + " [" + oldValue + "]:");
            if (value.isEmpty() && !oldValue.isEmpty()) {
                value = oldValue;
            }
            while ((!isFieldInputValid(f, canLeaveEmpty, value))) {
                value = io.readLine(f + " [" + oldValue + "]:");
            }
            if (!value.isEmpty()) {
                fields.put(f, value);
            }
        });
    }

    /**
     * Add tags read from user to reference.
     *
     * @param ref reference to tag
     */
    private void addTags(Reference ref) {
        io.println("Input tags separated by spaces, leave empty to add no tags");
        ref.addTag(readTags());
        io.println("Tags added.");
    }

    /**
     * Removes tags from Reference
     *
     * @param ref reference to un-tag
     */
    private void removeTags(Reference ref) {
        io.println("Input tags separated by spaces, leave empty to remove no tags");
        ref.removeTag(readTags());
        io.println("Tags removed");
    }

    /**
     * Read tags to be added/removed from input.
     *
     * @return tags read from input
     */
    private String[] readTags() {
        String input = io.readLine(": ");
        return input.split(" ");
    }

    /**
     * Prompt that asks the user a simple yes/no question and returns the value
     * as boolean.
     *
     * @param prompt question to ask the user
     * @return true if answer equals "y", false otherwise
     */
    private boolean askForPermission(String prompt) {
        String choice = io.readCharacter(prompt);
        return choice.toLowerCase().trim().equals("y");
    }

    /**
     * Imports references from the specified JSON file into the program.
     */
    private void loadReferences() {
        if (!references.getReferences().isEmpty()) {
            if (!askForPermission("Currently loaded references will be lost! Continue? [y/n]")) {
                return;
            }
        }
        String filename = io.readLine("filename [" + DEFAULT_FILENAME + "]:");
        if (filename.isEmpty()) {
            filename = DEFAULT_FILENAME;
        }
        try {
            references = JSONReader.loadReferences(filename);
        } catch (FileNotFoundException ex) {
            io.println("File not found!");
            return;
        } catch (JSONException ex) {
            Logger.getLogger(TextUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (references.getReferences().isEmpty()) {
            io.println("No references loaded!");
        } else {
            io.println("References loaded successfully!");
        }
    }

    /**
     * Exports references from the program into the specified JSON file.
     */
    private void saveReferences() {
        if (!references.getFilters().isEmpty()) {
            if (!askForPermission("Save only filtered references? [y/n]")) {
                references.clearFilters();
            }
        }
        if (references.getReferences().isEmpty()) {
            io.println("No references found!");
            return;
        }

        String filename = io.readLine("filename [" + DEFAULT_FILENAME + "]:");
        if (filename.isEmpty()) {
            filename = DEFAULT_FILENAME;
        }
        try {
            JSONWriter.saveReferences(references, filename);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(TextUI.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        io.println("References saved successfully!");
    }

    /**
     * Asks if the user's input to the specified field is valid.
     *
     * @param field the specified field
     * @param canLeaveEmpty can the field be left empty or not
     * @param input the user's own input to the field
     */
    private boolean isFieldInputValid(String field, boolean canLeaveEmpty, String input) {
        if (!canLeaveEmpty && input.isEmpty()) {
            return false;
        }
        return FieldValidator.validate(field, canLeaveEmpty, input);
    }

    private void manageFilters() {
        while (true) {
            if (references.getFilters().isEmpty()) {
                io.println("No filters applied");
            } else {
                io.println("Currently applied filters:");
                listFilters();
            }
            io.println("[a]dd filter, [c]lear filters, [q]uit");
            String choice = io.readCharacter(":");
            switch (choice.toLowerCase()) {
                case "a":
                    addFilter();
                    break;
                case "c":
                    clearFilters();
                    break;
                case "q":
                    return;
                default:
                    io.println("Invalid choice!");
                    break;
            }
        }
    }

    public void listFilters() {
        references.getFilters().stream().forEach((p) -> {
            io.println(p.toString());
        });
    }

    public Method[] getAvailableFilters() {
        Class fc = ReferenceFilters.class;
        return fc.getDeclaredMethods();
    }

    public void listAvailableFilters() {
        Method[] filters = getAvailableFilters();
        for (int i = 0; i < filters.length; i++) {
            io.println(i + "=" + filters[i].getName());
        }
    }

    private Method chooseFilter() {
        io.println("Choose filter:");
        listAvailableFilters();
        while (true) {
            int i = io.readInt(":");
            if (i < 0 || i >= getAvailableFilters().length) {
                io.println("Invalid choice");
                continue;
            }
            return getAvailableFilters()[i];
        }
    }

    private Object[] askForFilterParameters(Method filter) {
        Parameter[] params = filter.getParameters();
        ArrayList<Object> rp = new ArrayList<>();
        for (Parameter param : params) {
            if (param.getType() == String.class) {
                rp.add(io.readLine(param.getName() + ":"));
            } else if (param.getType() == ReferenceType.class) {
                rp.add(chooseReferenceType());
            }
        }
        return rp.toArray();
    }

    private void addFilter() {
        Method filter = chooseFilter();
        try {
            references.addFilter((Predicate<Reference>) filter.invoke(null, askForFilterParameters(filter)));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(TextUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearFilters() {
        references.clearFilters();
        io.println("Filters cleared!");
    }

    /**
     * Gets the References-object used. For testing.
     *
     * @return references
     */
    public References getReferences() {
        return references;
    }

}
