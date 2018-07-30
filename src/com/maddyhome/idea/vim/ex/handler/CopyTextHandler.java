/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CopyTextHandler extends CommandHandler {
  public CopyTextHandler() {
    super(new CommandName[]{
      new CommandName("co", "py"),
      new CommandName("t", "")
    }, RANGE_OPTIONAL | ARGUMENT_REQUIRED | WRITABLE);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      return executeVisual(editor, context, cmd);
    }

    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET);
    for (Caret caret : carets) {
      final TextRange range = cmd.getTextRange(editor, caret, context, false);
      final String text = EditorHelper.getText(editor, range.getStartOffset(), range.getEndOffset());

      executeCommon(editor, caret, context, cmd, text);
    }

    return true;
  }

  private boolean executeVisual(@NotNull Editor editor, @NotNull DataContext context,
                                @NotNull ExCommand cmd) throws ExException {
    final CaretModel caretModel = editor.getCaretModel();
    final TextRange range = CaretData.getVisualTextRange(caretModel.getPrimaryCaret());
    if (range == null) return false;

    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET);
    final List<String> texts = new ArrayList<>(caretModel.getCaretCount());
    for (int i = 0; i < carets.size(); i++) {
      final int start = EditorHelper.getLineStartForOffset(editor, range.getStartOffsets()[i]);
      final int end = EditorHelper.getLineEndForOffset(editor, range.getEndOffsets()[i]) + 1;
      final String text = EditorHelper.getText(editor, start, end);
      texts.add(text);
    }

    for (int i = 0; i < carets.size(); i++) {
      final int index = carets.size() - i - 1;
      final Caret caret = carets.get(i);
      final String text = texts.get(index);

      executeCommon(editor, caret, context, cmd, text);
    }

    return true;
  }

  private void executeCommon(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                             @NotNull ExCommand cmd, @NotNull String text) throws ExException {
    final ExCommand arg = CommandParser.getInstance().parse(cmd.getArgument());
    final int line = arg.getRanges().getFirstLine(editor, caret, context);
    final int offset = VimPlugin.getMotion().moveCaretToLineStart(editor, line + 1);

    VimPlugin.getCopy().putText(editor, caret, context, text, SelectionType.LINE_WISE, CommandState.SubMode.NONE,
                                offset, 1, true, false);
  }
}
