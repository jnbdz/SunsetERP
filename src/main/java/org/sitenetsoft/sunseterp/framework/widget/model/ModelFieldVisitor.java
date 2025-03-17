package org.sitenetsoft.sunseterp.framework.widget.model;

import org.sitenetsoft.sunseterp.framework.widget.model.ModelFormField.*;

/**
 *  A <code>ModelFormField</code> visitor.
 */
public interface ModelFieldVisitor {

    void visit(CheckField checkField) throws Exception;

    void visit(ContainerField containerField) throws Exception;

    void visit(DateFindField dateFindField) throws Exception;

    void visit(DateTimeField dateTimeField) throws Exception;

    void visit(DateRangePickerField dateRangePickerField) throws Exception;

    void visit(DisplayEntityField displayEntityField) throws Exception;

    void visit(DisplayField displayField) throws Exception;

    void visit(DropDownField dropDownField) throws Exception;

    void visit(FileField fileField) throws Exception;

    void visit(FormField formField) throws Exception;

    void visit(GridField gridField) throws Exception;

    void visit(HiddenField hiddenField) throws Exception;

    void visit(HyperlinkField hyperlinkField) throws Exception;

    void visit(IgnoredField ignoredField) throws Exception;

    void visit(ImageField imageField) throws Exception;

    void visit(LookupField lookupField) throws Exception;

    void visit(MenuField menuField) throws Exception;

    void visit(PasswordField passwordField) throws Exception;

    void visit(RadioField radioField) throws Exception;

    void visit(RangeFindField rangeFindField) throws Exception;

    void visit(ResetField resetField) throws Exception;

    void visit(ScreenField screenField) throws Exception;

    void visit(SubmitField submitField) throws Exception;

    void visit(TextareaField textareaField) throws Exception;

    void visit(TextField textField) throws Exception;

    void visit(TextFindField textFindField) throws Exception;
}
