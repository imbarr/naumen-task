import {Filter, TextInput} from 'react-admin';
import React from "react";

const PhoneFilter = props =>
    <Filter {...props}>
        <TextInput label='Name Substring' source='nameSubstring' alwaysOn/>
        <TextInput label='Phone Substring' source='phoneSubstring' alwaysOn/>
    </Filter>;

export default PhoneFilter