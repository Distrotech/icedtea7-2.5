/*
 * Copyright 1999-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * Copyright 2008, 2009 Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

#include "incls/_precompiled.incl"
#include "incls/_sharkValue.cpp.incl"

using namespace llvm;

// Cloning

SharkValue* SharkNormalValue::clone() const
{
  return SharkValue::create_generic(type(), generic_value(), zero_checked());
}
SharkValue* SharkAddressValue::clone() const
{
  return SharkValue::address_constant(address_value());
}

// Comparison

bool SharkNormalValue::equal_to(SharkValue *other) const
{
  return (this->type()          == other->type() &&
          this->generic_value() == other->generic_value() &&
          this->zero_checked()  == other->zero_checked());
}
bool SharkAddressValue::equal_to(SharkValue *other) const
{
  return (this->address_value() == other->address_value());
}

// Type access

ciType* SharkValue::type() const
{
  ShouldNotCallThis();
}
ciType* SharkNormalValue::type() const
{
  return _type;
}

BasicType SharkNormalValue::basic_type() const
{
  return type()->basic_type();
}
BasicType SharkAddressValue::basic_type() const
{
  return T_ADDRESS;
}

int SharkNormalValue::size() const
{
  return type()->size();
}
int SharkAddressValue::size() const
{
  return 1;
}

bool SharkValue::is_jint() const
{
  return false;
}
bool SharkValue::is_jlong() const
{
  return false;
}
bool SharkValue::is_jfloat() const
{
  return false;
}
bool SharkValue::is_jdouble() const
{
  return false;
}
bool SharkValue::is_jobject() const
{
  return false;
}
bool SharkValue::is_jarray() const
{
  return false;
}
bool SharkValue::is_address() const
{
  return false;
}

bool SharkNormalValue::is_jint() const
{
  return llvm_value()->getType() == SharkType::jint_type();
}
bool SharkNormalValue::is_jlong() const
{
  return llvm_value()->getType() == SharkType::jlong_type();
}
bool SharkNormalValue::is_jfloat() const
{
  return llvm_value()->getType() == SharkType::jfloat_type();
}
bool SharkNormalValue::is_jdouble() const
{
  return llvm_value()->getType() == SharkType::jdouble_type();
}
bool SharkNormalValue::is_jobject() const
{
  return llvm_value()->getType() == SharkType::jobject_type();
}
bool SharkNormalValue::is_jarray() const
{
  return basic_type() == T_ARRAY;
}
bool SharkAddressValue::is_address() const
{
  return true;
}

// Typed conversions from SharkValues

Value* SharkValue::jint_value() const
{
  ShouldNotCallThis();
}
Value* SharkValue::jlong_value() const
{
  ShouldNotCallThis();
}
Value* SharkValue::jfloat_value() const
{
  ShouldNotCallThis();
}
Value* SharkValue::jdouble_value() const
{
  ShouldNotCallThis();
}
Value* SharkValue::jobject_value() const
{
  ShouldNotCallThis();
}
Value* SharkValue::jarray_value() const
{
  ShouldNotCallThis();
}
int SharkValue::address_value() const
{
  ShouldNotCallThis();
}

Value* SharkNormalValue::jint_value() const
{
  assert(is_jint(), "should be");
  return llvm_value();
}
Value* SharkNormalValue::jlong_value() const
{
  assert(is_jlong(), "should be");
  return llvm_value();
}
Value* SharkNormalValue::jfloat_value() const
{
  assert(is_jfloat(), "should be");
  return llvm_value();
}
Value* SharkNormalValue::jdouble_value() const
{
  assert(is_jdouble(), "should be");
  return llvm_value();
}
Value* SharkNormalValue::jobject_value() const
{
  assert(is_jobject(), "should be");
  return llvm_value();
}
Value* SharkNormalValue::jarray_value() const
{
  assert(is_jarray(), "should be");
  return llvm_value();
}
int SharkAddressValue::address_value() const
{
  return _bci;
}

// Type-losing conversions -- use with care!

Value* SharkNormalValue::generic_value() const
{
  return llvm_value();
}
Value* SharkAddressValue::generic_value() const
{
  return LLVMValue::intptr_constant(address_value());
}

Value* SharkValue::intptr_value(SharkBuilder* builder) const
{
  ShouldNotCallThis();
}
Value* SharkNormalValue::intptr_value(SharkBuilder* builder) const
{
  return builder->CreatePtrToInt(jobject_value(), SharkType::intptr_type());
}

// Phi-style stuff

void SharkNormalValue::addIncoming(SharkValue *value, BasicBlock* block)
{
  assert(llvm::isa<llvm::PHINode>(generic_value()), "should be");
  ((llvm::PHINode *) generic_value())->addIncoming(
      value->generic_value(), block);
}
void SharkAddressValue::addIncoming(SharkValue *value, BasicBlock* block)
{
  assert(this->equal_to(value), "should be");
}

// Repeated null and divide-by-zero check removal

bool SharkValue::zero_checked() const
{
  ShouldNotCallThis();
}
void SharkValue::set_zero_checked(bool zero_checked) 
{
  ShouldNotCallThis();
}

bool SharkNormalValue::zero_checked() const
{
  return _zero_checked;
}
void SharkNormalValue::set_zero_checked(bool zero_checked)
{
  _zero_checked = zero_checked;
}