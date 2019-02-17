package com.boris.study.memory.logic.label;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.data.DataSaver;
import com.boris.study.memory.logic.data.LabelAssigner;
import com.boris.study.memory.logic.helpers.CommandsShower;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.utils.DataUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

public class LabelNavigator extends BotScenario {
    private static final String EMOJI_CROSS = "\u274c", EMOJI_CHECK = "\u2705";

    private LabelRepository labelRepository;
    private DataRepository dataRepository;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();

        int stageInitialized = 0;
        int stageDataSaved = 1;

        if (null == getStage() || getStage() < stageInitialized) {
            logger.info("LabelNavigator - Starting for " + client);
            init(request);
            setStage(stageInitialized);
            return false;
        }

        logger.info("LabelNavigator - Continuing for " + client);

        if (null != getStage() && getStage() == stageDataSaved) {
            String newDataUrl = request.get(DataSaver.Result.NEW_DATA_URL.name());

            // FIXME: 17.02.19 HOUSTON, WE NEED REFACTORING
            request.put(LabelAssigner.Key.URL.name(), newDataUrl);
            request.put(LabelAssigner.Key.LABEL_NAME.name(), getCurrentDir());
            processStateless(LabelAssigner.class, request);

            Label current = labelRepository.findByNameAndClientId(getCurrentDir(), client.getId())
                    .orElseThrow(() -> new IllegalStateException("No label '" + getCurrentDir() + "'"));
            updateNavigator(current, request);
            setStage(stageInitialized);
            return false;
        }

        if (request.update.hasCallbackQuery()) {
            String callback = request.update.getCallbackQuery().getData();
            Label callbackLabel = labelRepository.findByNameAndClientId(callback, client.getId())
                    .orElseThrow(() -> new IllegalStateException("No label '" + callback + "'"));
            updateNavigator(callbackLabel, request);

        } else if (request.update.hasMessage()) {
            if (request.update.getMessage().hasText()) {

                String text = request.update.getMessage().getText();
                if (dataUtils.isValidDataUrl(text)) {
                    if (!dataRepository.existsById(text)) {
                        try {
                            bot.execute(botUtils.markdownMessage(
                                    uiUtils.getErrors().getNoDataByUrl(),
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException e) {
                            logger.error("Couldn't send error to client + " + client, e);
                        }
                    } else {
                        Data data = dataRepository.findByUrl(text).get();
                        Label label = labelRepository.findByNameAndClientId(getCurrentDir(), client.getId()).get();
                        if (data.getLabels().contains(label)) {
                            try {
                                bot.execute(botUtils.plainMessage("Label '" + label.getName() +
                                                "' is already assigned to this data",
                                        botUtils.retrieveChat(request.update).getId()));
                            } catch (TelegramApiException e) {
                                logger.error(
                                        String.format(
                                                "Couldn't tell client %s that label %s is already assigned to %s",
                                                client, label, data),
                                        e);
                            }
                        } else {
                            request.put(LabelAssigner.Key.URL.name(), request.update.getMessage().getText());
                            request.put(LabelAssigner.Key.LABEL_NAME.name(), getCurrentDir());
                            processStateless(LabelAssigner.class, request);
                            Label current = labelRepository.findByNameAndClientId(getCurrentDir(), client.getId())
                                    .orElseThrow(() -> new IllegalStateException("No label '" + getCurrentDir() + "'"));
                            updateNavigator(current, request);
                        }
                    }
                } else if (text.equals("To main menu")) {
                    processStateless(CommandsShower.class, request);
                    setState(new JSONObject());
                    setStage(null);
                    logger.info("LabelNavigator - Finished for " + client);
                    return true;
                } else {
                    if (dataUtils.isValidLabelName(text)) {
                        String name = text.toLowerCase();
                        Label label = labelRepository.findByNameAndClientId(name, client.getId()).orElseGet(() -> {
                            try {
                                bot.execute(botUtils.plainMessage(
                                        "This label doesn't exist now, but I will create it",
                                        botUtils.retrieveChat(request.update).getId()));
                            } catch (TelegramApiException e) {
                                logger.error("Couldn't inform about creating new label in request " + request, e);
                            }

                            return new Label(name, client);
                        });

                        Label current = labelRepository.findByNameAndClientId(getCurrentDir(), client.getId())
                                .orElseThrow(() -> new IllegalStateException("No current label '" + getCurrentDir() +
                                        "' found for request " + request));

                        current.addSon(label);
                        labelRepository.save(label);
                        labelRepository.save(current);
                        try {
                            bot.execute(botUtils.plainMessage(
                                    "Successfully added a sublabel '" + name + "'",
                                    botUtils.retrieveChat(request.update).getId()));
                        } catch (TelegramApiException e) {
                            logger.error("Couldn't inform about a sublabel in request " + request, e);
                        }
                        updateNavigator(current, request);
                    } else {
                        try {
                            bot.execute(botUtils.plainMessage(
                                    "Didn't save new label: it's name's length must be less than " +
                                            DataUtils.LABEL_NAME_LENGTH +
                                            " symbols and it can't contain line breaks",
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException e) {
                            logger.error(
                                    String.format("Couldn't inform %s about wrong label name in request %s",
                                            client, request),
                                    e);
                        }
                    }
                }
            } else {
                if (!processOther(DataSaver.class, request)) {
                    setStage(stageDataSaved);
                }
            }
        }
        return false;
    }

    private void init(Request request) {
        Label allData = labelRepository.findByNameAndClientId("all data", getClient().getId())
                .orElseThrow(() -> new IllegalStateException("No 'all data' label"));
        SendMessage sendLabelInfo = botUtils.markdownMessage(
                "Now you can search for labels and data\n" +
                        "To select a label, click on it. To see it's sublabels and data, marked by it, " +
                        "click again (the label will be unselected).\n\n" +
                        "To assign current label to some data, send me its URL\n" +
                        "To save some label as a sublabel to the current one, send me its name.\n" +
                        "*Note: label's name's length must be less than " + DataUtils.LABEL_NAME_LENGTH
                        + " symbols and it can't contain line breaks*",
                botUtils.retrieveChat(request.update).getId()
        );
        sendLabelInfo.setReplyMarkup(new ReplyKeyboardMarkup().setKeyboard(
                Arrays.asList(
                        new KeyboardRow() {{
                            add(new KeyboardButton("Send data by selected label(s)"));
                        }},
                        new KeyboardRow() {{
                            add(new KeyboardButton("Rename label"));
                        }},
                        new KeyboardRow() {{
                            add(new KeyboardButton("Delete label"));
                        }},
                        new KeyboardRow() {{
                            add(new KeyboardButton("To main menu"));
                        }}
                )).setResizeKeyboard(true)
        );

        try {
            bot.execute(sendLabelInfo);

            updatePath("all data");
            updateNavigator(allData, request);
        } catch (TelegramApiException e) {
            logger.error("Failed to initialize LabelNavigator and send dat aof 'all data' in request " + request, e);
        }
    }

    private void updateNavigator(Label label, Request request) {
        Client client = getClient();

        if (getState().has(label.getName()) || isLabelInPath(label.getName())) { // if gonna move to this label
            updatePath(label.getName());
            if (getState().has("CALLBACK MESSAGE")) {
                DeleteMessage deleteMessage = new DeleteMessage()
                        .setMessageId(Integer.parseInt(getState().get("CALLBACK MESSAGE").toString()))
                        .setChatId(Long.parseLong(getState().get("CALLBACK CHAT").toString()));
                try {
                    bot.execute(deleteMessage);
                } catch (TelegramApiException e) {
                    logger.error("Couldn't delete old callback message in request " + request, e);
                }
            }

            if (getState().has(label.getName())) {
                getState().remove(label.getName());
                setState(getState());
            }

            SendMessage sendLabelContents = botUtils.plainMessage(
                    createDataList(label), botUtils.retrieveChat(request.update).getId()
            );
            sendLabelContents.setReplyMarkup(createSublabelsMarkup(label));

            try {
                Message callbackMessage = bot.execute(sendLabelContents);
                setState(getState().put("CALLBACK MESSAGE", callbackMessage.getMessageId()));
                setState(getState().put("CALLBACK CHAT", callbackMessage.getChatId()));
            } catch (TelegramApiException e) {
                logger.error("Failed to check sublabels of label info in request " + request, e);
            }
        } else {
            Label parent = labelRepository.findByNameAndClientId(
                    getCurrentDir(), client.getId()
            ).orElseThrow(() -> new IllegalStateException("Label '" + getCurrentDir() + "' does not exist"));

            setState(getState().put(label.getName(), "checked"));
            try {
                bot.execute(new EditMessageReplyMarkup()
                        .setMessageId(Integer.parseInt(getState().get("CALLBACK MESSAGE").toString()))
                        .setChatId(Long.parseLong(getState().get("CALLBACK CHAT").toString()))
                        .setReplyMarkup(createSublabelsMarkup(parent)));
            } catch (TelegramApiException e) {
                logger.error("Failed to check label info in request " + request, e);
            }
        }
    }

    private InlineKeyboardMarkup createSublabelsMarkup(Label label) {
        updatePath(label.getName());

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        if (null != label.getParents() && !label.getParents().isEmpty()) {
            InlineKeyboardButton levelUp = new InlineKeyboardButton("Go back to '" + getLevelUpDir() + "'");
            levelUp.setCallbackData(getLevelUpDir());
            buttons.add(Collections.singletonList(levelUp));
        }

        int labelsInRow = label.getSons().size() > 4 ? 3 : 2;
        List<Label> sons = new ArrayList<>(label.getSons());
        sons.sort(Comparator.comparing(Label::getName));

        for (int i = 0; i < sons.size() / labelsInRow + 1; i++) {
            List<InlineKeyboardButton> row = new LinkedList<>();
            for (int j = 0; j < labelsInRow && i * labelsInRow + j < sons.size(); j++) {
                Label son = sons.get(i * labelsInRow + j);
                String emoji = getState().has(son.getName()) ? EMOJI_CHECK : EMOJI_CROSS;
                InlineKeyboardButton labelButton = new InlineKeyboardButton(emoji + son.getName());
                labelButton.setCallbackData(son.getName());
                row.add(labelButton);
            }
            if (!row.isEmpty())
                buttons.add(row);
        }
        return new InlineKeyboardMarkup().setKeyboard(buttons);
    }

    private String createDataList(Label label) {
        StringBuilder dataContent =
                new StringBuilder("Here is a list of data, marked by '" + label.getName() + "'\n")
                        .append("-------------------------------------");
        int lengthWithoutData = dataContent.length();

        Set<String> labelNamesRecursive = label.getAllSonsRecursively().stream()
                .map(Label::getName)
                .collect(Collectors.toSet());

        Set<Data> labeledData =
                dataRepository.findAllByLabelNamesAndClientId(labelNamesRecursive, label.getClient().getId());

        int i = 1;
        for (Iterator<Data> it = labeledData.iterator(); it.hasNext(); i++) {
            Data data = it.next();
            dataContent.append("\n")
                    .append(i).append(". ") // number of data in the list
                    .append(data.getUrl()) // data's url
                    .append(" - ").append(data.getDescription()).append("\n"); // data's description
        }

        if (dataContent.length() == lengthWithoutData) {
            dataContent.append("\n").append("Well, no data yet");
        }

        return dataContent.toString();
    }

    private void updatePath(String labelName) {
        if (!getState().has("PATH")) {
            setState(getState().put("PATH", ""));
        }

        String currentPath = getState().get("PATH").toString();

        if (isLabelInPath(labelName)) { // TODO: 17.02.19 label names should not contain /
            setState(getState().put("PATH",
                    currentPath.substring(0, currentPath.indexOf(labelName) + labelName.length())));
        } else {
            setState(getState().put("PATH", currentPath + "/" + labelName));
        }
    }

    private boolean isLabelInPath(String labelName) {
        String currentPath = getState().get("PATH").toString();
        List<String> hierarchy = Arrays.stream(currentPath.split("/")).collect(Collectors.toList());

        return hierarchy.contains(labelName);
    } // TODO: 17.02.19 create LabelUtils for code like this

    private String getCurrentDir() {
        String currentPath = getState().get("PATH").toString();
        String[] hierarchy = currentPath.split("/");
        if (hierarchy.length > 0) {
            return hierarchy[hierarchy.length - 1];
        } else {
            return "NONE";
        }
    }

    private String getLevelUpDir() {
        String currentPath = getState().get("PATH").toString();
        String[] hierarchy = currentPath.split("/");
        if (hierarchy.length > 1) {
            return hierarchy[hierarchy.length - 2];
        } else {
            return "NONE";
        }
    }

    public LabelNavigator(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setLabelRepository(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(LabelNavigator.class);
}