/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */import * as React from 'react';
import { createApp } from 'ice'
import { ConfigProvider } from '@alifd/next';
import NotFound from '@/components/NotFound';
import PageLoading from '@/components/PageLoading';
import BasicLayout from '@/layouts/BasicLayout';
import { LANGUAGE_KEY, LANGUAGE_SWITCH, LANGUAGE_SWITCH_ALL_PAGE } from './constants';
import intl from 'react-intl-universal';
import { emit } from './emit.js'
import zh_CN from '@alifd/next/lib/locale/zh-cn';
import en_US from '@alifd/next/lib/locale/en-us';


const locales = {
  "en-US": require('./locales/en-US.json'),
  "zh-CN": require('./locales/zh-CN.json'),
};

class DubboDocApp extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
        dubboDocLang: zh_CN
    }
  }

  componentDidMount() {
    emit.on(LANGUAGE_SWITCH, (langChangeTo) => this.loadLocales(langChangeTo));
    if (!localStorage.getItem(LANGUAGE_KEY)) {
      localStorage.setItem(LANGUAGE_KEY, navigator.language === 'zh-CN' ? 'zh-CN' : 'en-US');
    }
    this.loadLocales(undefined); 
  }

  loadLocales (langChangeTo) {
    let currentLocale;
    if(langChangeTo){
      currentLocale = langChangeTo;
    } else {
      currentLocale = intl.determineLocale({
        localStorageLocaleKey : LANGUAGE_KEY
      });
    }
    
    intl.init({
      currentLocale: currentLocale, 
      locales,
    })
    .then(() => {
      this.setState({
        dubboDocLang: currentLocale === 'zh-CN' ? zh_CN : en_US
      });
      emit.emit(LANGUAGE_SWITCH_ALL_PAGE);
      // console.log('==== locales load success', currentLocale);
    });
  }
  

  render() {
    return (
      <ConfigProvider prefix="next-icestark-" locale={this.state.dubboDocLang}>{this.props.children}</ConfigProvider>
    );
  }

}


const appConfig = {
  app: {
    rootId: 'icestark-container',
    addProvider: ({ children }) => (
      <DubboDocApp children={children} />
    ),
  },
  router: {
    type: 'hash',
  },
  icestark: {
    type: 'framework',
    Layout: BasicLayout,
    getApps: async () => {
      const apps = [
      ];
      return apps;
    },
    appRouter: {
      NotFoundComponent: NotFound,
      LoadingComponent: PageLoading,
    },
  },
};
createApp(appConfig);