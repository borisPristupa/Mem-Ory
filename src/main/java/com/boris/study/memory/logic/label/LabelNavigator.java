package com.boris.study.memory.logic.label;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

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

        if (null == getStage() || getStage() < stageInitialized) {
            logger.info("LabelNavigator - Starting for " + client);
            init(request);
            setStage(stageInitialized);
            return false;
        }

        logger.info("LabelNavigator - Continuing for " + client);
        if (request.update.hasCallbackQuery()) {
            handleCallback(request);
            return false;
        }


        setStage(null);
        logger.info("LabelNavigator - Finished for " + client);
        return true;
    }

    private void init(Request request) {
        Client client = getClient();

        Optional<Label> allDataOp = labelRepository.findByNameAndClientId("all data", client.getId());
        Label allData = allDataOp.orElseThrow(() -> new IllegalStateException(String.format(
                "No 'all data' label found for user %s, request %s",
                client, request)));
        setState(getState().put("PATH", ""));

        SendMessage sendLabelInfo = botUtils.markdownMessage(
                "Search for labels and data. Send me text (without line breaks and its length should " +
                        "be less than 50 characters) to save it as a label. Send me a data URL to assign " +
                        "your current label to it",
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

        SendMessage sendLabelContents = botUtils.plainMessage(
                createDataList(allData), botUtils.retrieveChat(request.update).getId()
        );
        sendLabelContents.setReplyMarkup(createSublabelsMarkup(allData));

        try {
            bot.execute(sendLabelInfo);
            bot.execute(sendLabelContents);
        } catch (TelegramApiException e) {
            logger.error("Failed to initialize LabelNavigator and send dat aof 'all data' in request " + request, e);
        }
    }

    private void handleCallback(Request request) {
        Client client = getClient();

        Message oldContentsMessage = request.update.getCallbackQuery().getMessage();
        String callback = request.update.getCallbackQuery().getData();

        Label callbackLabel = labelRepository.findByNameAndClientId(callback, client.getId()
        ).orElseThrow(() -> new IllegalStateException("Label '" + callback + "' does not exist"));

        if (getState().has(callback) || isLabelInPath(callback)) {
            if (getState().has(callback)) {
                getState().remove(callback);
                setState(getState());
            }

            EditMessageText editMessageText = botUtils.plainEdit(
                    oldContentsMessage,
                    createDataList(callbackLabel)
            ).setReplyMarkup(createSublabelsMarkup(callbackLabel));

            try {
                bot.execute(editMessageText);
            } catch (TelegramApiRequestException e) {
                e.printStackTrace();
                System.err.println(e.getApiResponse());
            } catch (TelegramApiException e) {
                logger.error("Failed to check sublabels of label info in request " + request, e);
            }
        } else {
            Label parent = labelRepository.findByNameAndClientId(
                    getCurrentDir(), client.getId()
            ).orElseThrow(() -> new IllegalStateException("Label '" + getCurrentDir() + "' does not exist"));

            setState(getState().put(callback, "checked"));
            try {
                bot.execute(new EditMessageReplyMarkup()
                        .setChatId(oldContentsMessage.getChatId())
                        .setMessageId(oldContentsMessage.getMessageId())
                        .setReplyMarkup(createSublabelsMarkup(parent)));
            } catch (TelegramApiException e) {
                logger.error("Failed to check label info in request " + request, e);
            }
        }
    }

    private InlineKeyboardMarkup createSublabelsMarkup(Label label) {
        String currentPath = getState().get("PATH").toString();

        if (isLabelInPath(label.getName())) {
            setState(getState().put("PATH",
                    currentPath.substring(0, currentPath.indexOf(label.getName()) + label.getName().length())));
        } else {
            setState(getState().put("PATH", currentPath + "/" + label.getName()));
        }

        // TODO: 16.02.19 Capitalize reserved names, restrict using / in names, make users' labels lowerCased

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        if (null != label.getParents() && !label.getParents().isEmpty()) {
            InlineKeyboardButton levelUp = new InlineKeyboardButton("Go back to '" + getLevelUpDir() + "'");
            levelUp.setCallbackData(getLevelUpDir());
            buttons.add(Collections.singletonList(levelUp));
        }

        int labelsInRow = label.getSons().size() > 4 ? 3 : 2;
        List<Label> sons = new ArrayList<>(label.getSons());

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

    private boolean isLabelInPath(String label) {
        String currentPath = getState().get("PATH").toString();
        List<String> hierarchy = Arrays.stream(currentPath.split("/")).collect(Collectors.toList());

        return hierarchy.contains(label);
    }

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