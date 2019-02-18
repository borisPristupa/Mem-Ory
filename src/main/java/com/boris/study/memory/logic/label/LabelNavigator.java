package com.boris.study.memory.logic.label;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.ScenarioConfig;
import com.boris.study.memory.logic.data.DataSaver;
import com.boris.study.memory.logic.data.LabelAssigner;
import com.boris.study.memory.logic.helpers.CommandsShower;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.utils.DataUtils;
import com.boris.study.memory.utils.LabelUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: 18.02.19 REFACTOR OR DIE
public class LabelNavigator extends BotScenario {
    private static final String EMOJI_CROSS = "\u274c", EMOJI_CHECK = "\u2705", EMOJI_LEFT = "\u2b05";

    private LabelRepository labelRepository;
    private DataRepository dataRepository;
    private LabelUtils labelUtils;

    private static ScenarioConfig urlConfig, txtConfig, clickConfig;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        if (null == urlConfig)
            initConfig();

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
            String currentName = getCurrentLabelName();

            assignLabelToData(currentName, newDataUrl, request);
            updateNavigator(getCurrentLabel(), request);

            setStage(stageInitialized);
            return false;
        }

        if (request.update.hasCallbackQuery()) {

            String callback = request.update.getCallbackQuery().getData();
            String key = callback.split(" ")[0], value = callback.substring(callback.indexOf(" ") + 1);

            if ("L".equals(key)) {
                Label callbackLabel = labelUtils.obtain(value, client.getId());
                if ("delete".equals(getConfigState(clickConfig))) {
                    if (getCurrentLabel().getSons().contains(callbackLabel)) {
                        getCurrentLabel().getSons().remove(callbackLabel);
                        callbackLabel.getParents().remove(getCurrentLabel());
                    } else {
                        getCurrentLabel().getParents().remove(callbackLabel);
                        callbackLabel.getSons().remove(getCurrentLabel());
                    }

                    labelRepository.save(getCurrentLabel());
                    labelRepository.save(callbackLabel);
                    updateNavigator(getCurrentLabel(), request);
                } else
                    updateNavigator(callbackLabel, request);
            } else {
                ScenarioConfig config;

                if (urlConfig.getConfigName().equals(key)) {

                    config = urlConfig;
                } else if (txtConfig.getConfigName().equals(key)) {

                    config = txtConfig;
                } else if (clickConfig.getConfigName().equals(key)) {

                    config = clickConfig;
                } else {
                    throw new IllegalStateException("Unknown callback key " + key);
                }

                if (!getConfigState(config).equals(value)) {
                    setConfigState(config, value);

                    EditMessageReplyMarkup updateConfigMessage = new EditMessageReplyMarkup()
                            .setMessageId(request.update.getCallbackQuery().getMessage().getMessageId())
                            .setChatId(request.update.getCallbackQuery().getMessage().getChatId())
                            .setReplyMarkup(configMarkup(config));
                    try {
                        bot.execute(updateConfigMessage);
                    } catch (TelegramApiException e) {
                        logger.error("Failed to update config's message " + config + " " + request, e);
                    }
                }
            }
        } else if (request.update.hasMessage()) {
            if (request.update.getMessage().hasText()) {

                String text = request.update.getMessage().getText();
                if ("Send data by selected label(s)".equals(text)) {
                    Set<String> selectedLabels = getCheckedLabelsNames();
                    Set<Data> selectedData = dataRepository.findAllByLabelNamesAndClientId(selectedLabels, client.getId());
                    String dataList = dataUtils.formDataList(selectedData);
                    try {
                        bot.execute(botUtils.plainMessage(dataList, botUtils.retrieveChat(request.update).getId()));
                    } catch (TelegramApiException e) {
                        logger.error("Failed to send labeled data list in request " + request, e);
                    }
                } else if ("Delete label".equals(text)) {
                    Label currentLabel = getCurrentLabel();
                    if (currentLabel.getName().equals("all data")) {
                        try {
                            bot.execute(botUtils.markdownMessage(
                                    "Cannot delete *all data* label - it's too important for all of us",
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException exception) {
                            logger.error(
                                    "Couldn't warn user about inability to delete 'all data' in request " + request,
                                    exception);
                        }
                    } else {
                        Label allData = labelUtils.obtain("all data", client.getId());
                        currentLabel.getSons().forEach(label -> {
                            if (label.getParents().size() == 1) {
                                allData.addSon(label);
                            }
                        });
                        labelRepository.save(allData);
                        labelRepository.delete(currentLabel);
                        labelRepository.flush();
                        updateNavigator(labelUtils.obtain("all data", client.getId()), request);
                        try {
                            bot.execute(botUtils.plainMessage(
                                    "Deleted '" + currentLabel.getName() + "'",
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException exception) {
                            logger.error(
                                    "Couldn't inform about label deletion in request " + request,
                                    exception);
                        }
                    }
                } else if ("Configuration".equals(text)) {

                    sendConfig(clickConfig, request);
                    sendConfig(txtConfig, request);
                    sendConfig(urlConfig, request);
                } else if ("To main menu".equals(text)) {

                    processStateless(CommandsShower.class, request);
                    setState(new JSONObject());
                    setStage(null);
                    logger.info("LabelNavigator - Finished for " + client);
                    return true;
                } else if (dataUtils.isValidDataUrl(text)) {
                    if (!dataRepository.existsById(text)) {
                        try {
                            bot.execute(botUtils.markdownMessage(
                                    uiUtils.getErrors().getNoDataByUrl(),
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException e) {
                            logger.error("Couldn't send error to client + " + client, e);
                        }
                    } else { // treated as a name of a label
                        Data data = dataRepository.findByUrl(text).get();
                        Label label = labelUtils.obtain(getCurrentLabelName(), client.getId());

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
                            assignLabelToData(getCurrentLabelName(), request.update.getMessage().getText(), request);
                            updateNavigator(getCurrentLabel(), request);
                        }
                    }
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

                        Label current = labelRepository
                                .findByNameAndClientId(
                                        getCurrentLabelName(),
                                        client.getId())
                                .orElseThrow(() -> new IllegalStateException("No current label '" +
                                        getCurrentLabelName() +
                                        "' found for request " + request));

                        current.addSon(label);
                        labelRepository.save(label);
                        labelRepository.save(current);
                        try {
                            bot.execute(botUtils.plainMessage(
                                    "Successfully added a sublabel '" + name + "' for '" + current.getName() + "'",
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
        SendMessage sendLabelInfo = botUtils.markdownMessage(
                uiUtils.getLabelNavigationInfo(),
                botUtils.retrieveChat(request.update).getId());

        sendLabelInfo.setReplyMarkup(botUtils.oneColumnReplyMarkup(Arrays.asList(
                "Send data by selected label(s)",
                "Delete label",
                "Configuration",
                "To main menu"
        )));

        try {
            bot.execute(sendLabelInfo);
            updateNavigator(labelUtils.obtain("all data", getClient().getId()), request);
        } catch (TelegramApiException e) {
            logger.error("Failed to initialize LabelNavigator and send dat aof 'all data' in request " + request, e);
        }
    }

    private void updateNavigator(Label label, Request request) {
        // if gonna move to this label or it is the current one
        if (getState().has(label.getName()) || // it was checked
                label.getParents().isEmpty() || // it is top-level
                label.equals(getCurrentLabel()) || // it is the current one
                getCurrentLabel().getAllParentsRecursively().stream().anyMatch(label::equals)) { // it is an ancestor
            setCurrentLabel(label.getName());
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
                    labelUtils.createDataList(label), botUtils.retrieveChat(request.update).getId());
            sendLabelContents.setReplyMarkup(createEmojiMarkup(label));

            try {
                Message callbackMessage = bot.execute(sendLabelContents);
                setState(getState().put("CALLBACK MESSAGE", callbackMessage.getMessageId()));
                setState(getState().put("CALLBACK CHAT", callbackMessage.getChatId()));
            } catch (TelegramApiException e) {
                logger.error("Failed to check sublabels of label info in request " + request, e);
            }
        } else { // just selected a label
            Label parent = labelRepository.findByNameAndClientId(
                    getCurrentLabelName(), getClient().getId()
            ).orElseThrow(() -> new IllegalStateException("Label '"
                    + getCurrentLabelName() + "' does not exist"));

            setState(getState().put(label.getName(), "selected"));
            try {
                bot.execute(new EditMessageReplyMarkup()
                        .setMessageId(Integer.parseInt(getState().get("CALLBACK MESSAGE").toString()))
                        .setChatId(Long.parseLong(getState().get("CALLBACK CHAT").toString()))
                        .setReplyMarkup(createEmojiMarkup(parent)));
            } catch (TelegramApiException e) {
                logger.error("Failed to check label info in request " + request, e);
            }
        }
    }

    private InlineKeyboardMarkup createEmojiMarkup(Label label) {
        InlineKeyboardMarkup sublabelsMarkup = labelUtils.createNavigationMarkup(label);
        sublabelsMarkup.getKeyboard().forEach(inlineKeyboardButtons ->
                inlineKeyboardButtons.forEach(button -> {
                    String emoji;
                    if (getState().has(button.getText()))
                        emoji = EMOJI_CHECK;
                    else if (button.getText().matches(".*[A-Z].*")) {
                        emoji = EMOJI_LEFT;
                    } else {
                        emoji = EMOJI_CROSS;
                    }
                    button.setText(emoji + button.getText());
                }));
        return sublabelsMarkup;
    }

    private Set<String> getCheckedLabelsNames() {
        return getState().keySet().stream().filter(s -> s.equals(s.toLowerCase())).collect(Collectors.toSet());
    }

    private void initConfig() {
        urlConfig = new ScenarioConfig("URL", uiUtils.getConfig().getUrl(),
                Arrays.asList("send", "describe", "label", "delete"));
        if (!getState().has(urlConfig.getConfigName()))
            setConfigState(urlConfig, "label");
        else
            setConfigState(urlConfig, getConfigState(urlConfig));

        txtConfig = new ScenarioConfig("TXT", uiUtils.getConfig().getTxt(),
                Arrays.asList("goto", "rename", "sub", "super"));
        if (!getState().has(txtConfig.getConfigName()))
            setConfigState(txtConfig, "sub");
        else
            setConfigState(txtConfig, getConfigState(txtConfig));

        clickConfig = new ScenarioConfig("CLICK", uiUtils.getConfig().getClick(),
                Arrays.asList("select & goto", "delete"));
        if (!getState().has(clickConfig.getConfigName())) {
            setConfigState(clickConfig, "select & goto");
        } else
            setConfigState(clickConfig, getConfigState(clickConfig));
    }

    private void setConfigState(ScenarioConfig config, String state) {
        setState(getState().put(config.getConfigName(), state));
    }

    private String getConfigState(ScenarioConfig config) {
        return getState().get(config.getConfigName()).toString();
    }

    private InlineKeyboardMarkup configMarkup(ScenarioConfig config) {
        return new InlineKeyboardMarkup().setKeyboard(
                Collections.singletonList(config.getParameters().stream()
                        .map(s ->
                                new InlineKeyboardButton(
                                        (getConfigState(config).equals(s) ? EMOJI_CHECK : "") + s
                                ).setCallbackData(config.getConfigName() + " " + s))
                        .collect(Collectors.toList())));
    }

    private void sendConfig(ScenarioConfig config, Request request) {
        SendMessage sendMessage = botUtils.markdownMessage(
                config.getDescription(),
                botUtils.retrieveChat(request.update).getId()
        ).setReplyMarkup(configMarkup(config));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiRequestException e) {
            logger.error(e.getApiResponse(), e);
        } catch (TelegramApiException e) {
            logger.error("Couldn't send config message: " + config + " " + getClient(), e);
        }
    }

    private void assignLabelToData(String label, String dataUrl, Request request) {
        request.put(LabelAssigner.Key.URL.name(), dataUrl);
        request.put(LabelAssigner.Key.LABEL_NAME.name(), label);
        processStateless(LabelAssigner.class, request);
    }

    private String getCurrentLabelName() {
        return getState().get("CURRENT").toString();
    }

    private Label getCurrentLabel() {
        return labelUtils.obtain(getCurrentLabelName(), getClient().getId());
    }

    private void setCurrentLabel(String labelName) {
        setState(getState().put("CURRENT", labelName));
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

    @Autowired
    public void setLabelUtils(LabelUtils labelUtils) {
        this.labelUtils = labelUtils;
    }

    private final Logger logger = LoggerFactory.getLogger(LabelNavigator.class);
}