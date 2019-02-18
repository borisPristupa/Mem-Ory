package com.boris.study.memory.utils;

import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LabelUtils {
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private DataUtils dataUtils;

    public Label obtain(String name, Long clientId) throws IllegalStateException {
        return labelRepository.findByNameAndClientId(name, clientId)
                .orElseThrow(() -> new IllegalStateException("No label '" + name + "'"));
    }

    // Only works correctly if you update your path to this label first
    public InlineKeyboardMarkup createNavigationMarkup(Label label) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        if (null != label.getParents() && !label.getParents().isEmpty()) {
            List<Label> parents = new ArrayList<>(label.getParents());
            parents.sort(Comparator.comparing(Label::getName));
            parents.forEach(parent -> {
                InlineKeyboardButton levelUp = new InlineKeyboardButton("Go back to '" +
                        parent.getName() + "'");
                levelUp.setCallbackData("L " + parent.getName());
                buttons.add(Collections.singletonList(levelUp));
            });
        }

        int labelsInRow = label.getSons().size() > 4 ? 3 : 2;
        List<Label> sons = new ArrayList<>(label.getSons());
        sons.sort(Comparator.comparing(Label::getName));

        for (int i = 0; i < sons.size() / labelsInRow + 1; i++) {
            List<InlineKeyboardButton> row = new LinkedList<>();
            for (int j = 0; j < labelsInRow && i * labelsInRow + j < sons.size(); j++) {
                Label son = sons.get(i * labelsInRow + j);
                InlineKeyboardButton labelButton = new InlineKeyboardButton(son.getName());
                labelButton.setCallbackData("L " + son.getName());
                row.add(labelButton);
            }
            if (!row.isEmpty())
                buttons.add(row);
        }
        return new InlineKeyboardMarkup().setKeyboard(buttons);
    }

    public String createDataList(Label label) {
        StringBuilder dataContent =
                new StringBuilder("Here is a list of data, marked by '" + label.getName() + "'\n")
                        .append("-------------------------------------");

        Set<String> labelNamesRecursive = label.getAllSonsRecursively().stream()
                .map(Label::getName)
                .collect(Collectors.toSet());
        labelNamesRecursive.add(label.getName());

        Set<Data> labeledData = dataRepository
                .findAllByLabelNamesAndClientId(labelNamesRecursive, label.getClient().getId());

        dataContent.append("\n").append(dataUtils.formDataList(labeledData));

        return dataContent.toString();
    }

}
