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
 */
import React from 'react';
import { AppLink } from '@ice/stark';
import IceContainer from '@icedesign/container';

import styles from './index.module.scss';

export default () => {
  return (
    <div className={styles.basicnotfound}>
      <IceContainer>
        <div className={styles.exceptioncontent}>
          <img
            src="https://img.alicdn.com/tfs/TB1txw7bNrI8KJjy0FpXXb5hVXa-260-260.png"
            className={styles.imgException}
            alt="页面不存在"
          />
          <div className="prompt">
            <h3 className={styles.title}>抱歉，你访问的页面不存在</h3>
            <p className={styles.description}>
              您要找的页面没有找到，请返回
              <AppLink to="/">首页</AppLink>
              继续浏览
            </p>
          </div>
        </div>
      </IceContainer>
    </div>
  );
};
