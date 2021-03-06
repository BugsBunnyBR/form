/**
MIT License

Copyright (c) 2018 Youse Seguros

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package br.com.youse.forms.form

import br.com.youse.forms.form.models.FormField
import br.com.youse.forms.form.models.ObservableValue
import br.com.youse.forms.validators.ValidationMessage
import br.com.youse.forms.validators.ValidationStrategy
import kotlin.test.Test
import kotlin.test.BeforeTest

import kotlin.test.assertEquals

class TestFieldValidationChange<T> : IForm.FieldValidationChange<T> {
    private val validationsList: MutableList<Pair<T, List<ValidationMessage>>> = mutableListOf()

    override fun onFieldValidationChange(key: T, validations: List<ValidationMessage>) {
        validationsList.add(Pair(key, validations))
    }

    fun assertKeyAt(index: Int, key: T) {
        assertEquals(validationsList[index].first, key)
    }

    fun assertValidationsAt(index: Int, validations: List<ValidationMessage>) {
        assertEquals(validationsList[index].second, validations)
    }

    fun assertValidationPair(index: Int, validations: Pair<T, List<ValidationMessage>>): TestFieldValidationChange<T> {
        assertKeyAt(index, validations.first)
        assertValidationsAt(index, validations.second)
        return this
    }

    fun assertSize(size: Int): TestFieldValidationChange<T> {
        assertEquals(validationsList.size, size)
        return this
    }

    fun assertNoValue(): TestFieldValidationChange<T> {
        assertSize(0)
        return this
    }
}


class TestFormValidationChange : IForm.FormValidationChange {
    private val changes = mutableListOf<Boolean>()
    override fun onFormValidationChange(isValid: Boolean) {
        changes.add(isValid)
    }

    fun assertChange(index: Int, isValid: Boolean): TestFormValidationChange {
        assertEquals(changes[index], isValid)
        return this
    }

    fun assertSize(size: Int): TestFormValidationChange {
        assertEquals(changes.size, size)
        return this
    }

    fun assertNoValue(): TestFormValidationChange {
        assertSize(0)
        return this
    }
}

class TestValidSubmit<T> : IForm.ValidSubmit<T> {
    private val submits = mutableListOf<List<Pair<T, Any?>>>()
    override fun onValidSubmit(fields: List<Pair<T, Any?>>) {
        submits.add(fields)
    }

    fun assertSize(size: Int): TestValidSubmit<T> {
        assertEquals(submits.size, size)
        return this
    }

    fun assertNoValue(): TestValidSubmit<T> {
        assertSize(0)
        return this
    }
}

class TestSubmitFailed<T> : IForm.SubmitFailed<T> {
    private val failedSubmits = mutableListOf<List<Pair<T, List<ValidationMessage>>>>()
    override fun onSubmitFailed(validations: List<Pair<T, List<ValidationMessage>>>) {
        failedSubmits.add(validations)
    }

    fun assertValidations(index: Int, validations: List<Pair<T, List<ValidationMessage>>>): TestSubmitFailed<T> {
        assertEquals(failedSubmits[index], validations)
        return this
    }

    fun assertSize(size: Int): TestSubmitFailed<T> {
        assertEquals(failedSubmits.size, size)
        return this
    }

    fun assertNoValue(): TestSubmitFailed<T> {
        assertSize(0)
        return this
    }
}

class FormTest {

    private lateinit var emailObservable: ObservableValue<String>
    private lateinit var passwordObservable: ObservableValue<String>
    private lateinit var ageObservable: ObservableValue<Int>

    @BeforeTest
    fun setup() {
        emailObservable = ObservableValue()
        passwordObservable = ObservableValue()
        ageObservable = ObservableValue()
    }

    private fun buildForm(strategy: ValidationStrategy = ValidationStrategy.AFTER_SUBMIT,
                          email: String,
                          password: String,
                          age: Int): IForm.Builder<Int> {

        emailObservable.value = email
        passwordObservable.value = password
        ageObservable.value = age

        return Form.Builder<Int>(strategy)
                .addField(FormField(EMAIL_ID, emailObservable, validators = emailValidators))
                .addField(FormField(PASSWORD_ID, passwordObservable, validators = passwordValidators))
                .addField(FormField(AGE_ID, ageObservable, validators = ageValidators))

    }

    @Test
    fun shouldResetFormState() {
        val fieldChange = TestFieldValidationChange<Int>()
        val formChange = TestFormValidationChange()
        val validSubmit = TestValidSubmit<Int>()
        val failedSubmit = TestSubmitFailed<Int>()

        val form = buildForm(ValidationStrategy.AFTER_SUBMIT, "foo", "bar", MIN_AGE_VALUE - 1)
                .setFieldValidationListener(fieldChange)
                .setFormValidationListener(formChange)
                .setValidSubmitListener(validSubmit)
                .setSubmitFailedListener(failedSubmit)
                .build()

        formChange.assertNoValue()
        fieldChange.assertNoValue()

        form.doSubmit()

        fieldChange.assertSize(3)
        formChange.assertChange(0, false)
        formChange.assertSize(1)

        form.reset()

        fieldChange.assertSize(3)
        formChange.assertSize(1)

        emailObservable.value = VALID_EMAIL
        passwordObservable.value = VALID_PASSWORD
        ageObservable.value = MIN_AGE_VALUE

        fieldChange.assertSize(3)
        formChange.assertSize(1)

        form.doSubmit()

        fieldChange.assertSize(6)
        formChange.assertSize(2)

    }

    @Test
    fun shouldValidateAllTheTime() {

        val validations = listOf(Pair(EMAIL_ID, listOf(INVALID_EMAIL_MESSAGE)),
                Pair(PASSWORD_ID, listOf(INVALID_PASSWORD_MESSAGE)),
                Pair(AGE_ID, listOf(TOO_SMALL_MESSAGE)))

        val fieldChange = TestFieldValidationChange<Int>()
        val formChange = TestFormValidationChange()
        val validSubmit = TestValidSubmit<Int>()
        val failedSubmit = TestSubmitFailed<Int>()

        buildForm(ValidationStrategy.ALL_TIME, "foo", "bar", MIN_AGE_VALUE - 1)
                .setFieldValidationListener(fieldChange)
                .setFormValidationListener(formChange)
                .setValidSubmitListener(validSubmit)
                .setSubmitFailedListener(failedSubmit)
                .build()


        validations.forEachIndexed { index, pair ->
            fieldChange.assertValidationPair(index, pair)
        }

        fieldChange.assertSize(validations.size)

        formChange
                .assertChange(0, false)
                .assertSize(1)

        validSubmit.assertNoValue()

        failedSubmit.assertNoValue()

    }

    @Test
    fun shouldExecuteValidationAfterSubmit() {

        val fieldChange = TestFieldValidationChange<Int>()
        val formChange = TestFormValidationChange()
        val validSubmit = TestValidSubmit<Int>()
        val failedSubmit = TestSubmitFailed<Int>()

        val form = buildForm(
                ValidationStrategy.AFTER_SUBMIT,
                "foo",
                "bar",
                MIN_AGE_VALUE - 1)
                .setFieldValidationListener(fieldChange)
                .setFormValidationListener(formChange)
                .setValidSubmitListener(validSubmit)
                .setSubmitFailedListener(failedSubmit)
                .build()

        emailObservable.value = "bar"
        passwordObservable.value = "foo"
        ageObservable.value = MAX_AGE_VALUE + 1

        fieldChange.assertNoValue()
        formChange.assertNoValue()
        failedSubmit.assertNoValue()
        validSubmit.assertNoValue()

        form.doSubmit()

        fieldChange.assertSize(3)
        formChange.assertSize(1)
        formChange.assertChange(0, false)
        failedSubmit.assertSize(1)
        validSubmit.assertNoValue()

        emailObservable.value = VALID_EMAIL
        passwordObservable.value = VALID_PASSWORD
        ageObservable.value = MIN_AGE_VALUE

        fieldChange.assertSize(6)
        formChange.assertSize(2)
        failedSubmit.assertSize(1)
        validSubmit.assertNoValue()

        form.doSubmit()

        failedSubmit.assertSize(1)
        validSubmit.assertSize(1)
    }

    @Test
    fun shouldCallOnFieldValidationChangeDueFieldValidation() {
        val fieldChanges = TestFieldValidationChange<Int>()
        val form = Form.Builder<Int>()
                .addField(FormField(AGE_ID, ageObservable, validators = ageValidators, validationTriggers = emptyList()))
                .setFieldValidationListener(fieldChanges)
                .build()

        ageObservable.value = MIN_AGE_VALUE - 1

        form.doSubmit()

        fieldChanges.assertValidationsAt(0, listOf(TOO_SMALL_MESSAGE))

        ageObservable.value = MAX_AGE_VALUE + 1

        fieldChanges.assertValidationsAt(1, listOf(TOO_LARGE_MESSAGE))

        ageObservable.value = MIN_AGE_VALUE

        fieldChanges.assertValidationsAt(2, listOf())

        fieldChanges.assertSize(3)

    }
}